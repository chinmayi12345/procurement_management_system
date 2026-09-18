package com.infosys.procurementsystem.service.impl;

import com.infosys.procurementsystem.entity.Department;
import com.infosys.procurementsystem.entity.Product;
import com.infosys.procurementsystem.entity.PurchaseRequest;
import com.infosys.procurementsystem.entity.User;
import com.infosys.procurementsystem.enums.RequestStatus;
import com.infosys.procurementsystem.enums.Role;
import com.infosys.procurementsystem.exception.BadRequestException;
import com.infosys.procurementsystem.exception.ForbiddenException;
import com.infosys.procurementsystem.exception.ResourceNotFoundException;
import com.infosys.procurementsystem.repository.ProductRepository;
import com.infosys.procurementsystem.repository.PurchaseRequestRepository;
import com.infosys.procurementsystem.repository.PaymentRepository;
import com.infosys.procurementsystem.entity.Payment;
import com.infosys.procurementsystem.enums.PaymentStatus;
import com.infosys.procurementsystem.repository.UserRepository;
import com.infosys.procurementsystem.service.EmailService;
import com.infosys.procurementsystem.service.PurchaseRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class PurchaseRequestServiceImpl implements PurchaseRequestService {

    private final PurchaseRequestRepository purchaseRequestRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final EmailService emailService;
    private final PaymentRepository paymentRepository;

    @Override
    @Transactional
    public PurchaseRequest createPurchaseRequest(PurchaseRequest request, Long requesterId) {
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new ResourceNotFoundException("Requester user not found with id: " + requesterId));

        if (requester.getDepartment() == null) {
            throw new BadRequestException("User does not belong to a department. Cannot submit purchase request.");
        }

        request.setRequester(requester);
        request.setDepartment(requester.getDepartment());
        request.setStatus(RequestStatus.DRAFT);
        request.setCurrentApprover(null);

        BigDecimal total = BigDecimal.ZERO;
        for (PurchaseRequest.PurchaseRequestItem item : request.getItems()) {
            Product product = productRepository.findById(item.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + item.getProduct().getId()));
            
            item.setProduct(product);
            item.setPrice(product.getPrice());
            BigDecimal lineTotal = product.getPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()));
            item.setTotalPrice(lineTotal);
            total = total.add(lineTotal);
        }
        request.setTotalAmount(total);

        return purchaseRequestRepository.save(request);
    }

    @Override
    @Transactional
    public PurchaseRequest submitPurchaseRequest(Long id, Long requesterId) {
        PurchaseRequest request = getPurchaseRequestById(id);

        if (!request.getRequester().getId().equals(requesterId)) {
            throw new ForbiddenException("Only the requester can submit this purchase request");
        }

        if (request.getStatus() != RequestStatus.DRAFT) {
            throw new BadRequestException("Only DRAFT purchase requests can be submitted");
        }

        User departmentAdmin = request.getDepartment().getAdmin();
        if (departmentAdmin == null) {
            throw new BadRequestException(
                    "Department '" + request.getDepartment().getName() + "' has no admin assigned yet. " +
                    "Ask an administrator to assign one before submitting requests.");
        }

        // A single department admin approves/rejects the request directly.
        request.setCurrentApprover(departmentAdmin);
        request.setStatus(RequestStatus.PENDING_APPROVAL);

        PurchaseRequest savedRequest = purchaseRequestRepository.save(request);

        // Notify the requester that their request was raised successfully,
        // and notify the admin that a new request is awaiting their approval.
        emailService.sendRequestRaisedEmailToRequester(savedRequest);
        emailService.sendRequestRaisedEmailToAdmin(savedRequest);

        return savedRequest;
    }

    @Override
    @Transactional
    public PurchaseRequest approvePurchaseRequest(Long id, Long approverId) {
        PurchaseRequest request = getPurchaseRequestById(id);
        validateIsDepartmentAdmin(request, approverId);

        if (request.getStatus() != RequestStatus.PENDING_APPROVAL) {
            throw new BadRequestException("Purchase request is not in PENDING_APPROVAL status");
        }

        request.setStatus(RequestStatus.APPROVED);
        request.setCurrentApprover(null);

        PurchaseRequest savedRequest = purchaseRequestRepository.save(request);

        // The supplier is determined from the selected product(s), never typed
        // by the user. A request currently supports one payment record, so all
        // items in a request must belong to the same supplier.
        if (savedRequest.getItems() == null || savedRequest.getItems().isEmpty()) {
            throw new BadRequestException("Approved purchase request must contain at least one product");
        }
        Long supplierId = savedRequest.getItems().get(0).getProduct().getSupplier().getId();
        boolean mixedSuppliers = savedRequest.getItems().stream()
                .anyMatch(item -> !supplierId.equals(item.getProduct().getSupplier().getId()));
        if (mixedSuppliers) {
            throw new BadRequestException("All products in one purchase request must belong to the same supplier");
        }

        com.infosys.procurementsystem.entity.Supplier supplier = savedRequest.getItems().get(0).getProduct().getSupplier();
        Payment payment = paymentRepository.findByPurchaseRequestId(savedRequest.getId()).orElseGet(Payment::new);
        payment.setPurchaseRequest(savedRequest);
        payment.setSupplier(supplier);
        payment.setUser(savedRequest.getRequester());
        payment.setAmount(savedRequest.getTotalAmount());
        payment.setUpiId(supplier.getUpiId());
        payment.setStatus(PaymentStatus.DETAILS_SENT);
        payment.setDetailsSentAt(java.time.LocalDateTime.now());
        paymentRepository.save(payment);

        // Notify the requester that their request has been approved.
        log.info("Purchase request #{} approved by user #{}; sending approval email to {}",
            savedRequest.getId(), approverId, savedRequest.getRequester().getEmail());
        emailService.sendRequestApprovedEmail(savedRequest);

        // Notify the supplier(s) of the ordered products so they can send
        // payment details next.
        emailService.sendRequestApprovedEmailToSuppliers(savedRequest);

        return savedRequest;
    }

    @Override
    @Transactional
    public PurchaseRequest rejectPurchaseRequest(Long id, Long approverId) {
        PurchaseRequest request = getPurchaseRequestById(id);
        validateIsDepartmentAdmin(request, approverId);

        if (request.getStatus() != RequestStatus.PENDING_APPROVAL) {
            throw new BadRequestException("Purchase request is not in PENDING_APPROVAL status");
        }

        request.setStatus(RequestStatus.REJECTED);
        request.setCurrentApprover(null);

        PurchaseRequest savedRequest = purchaseRequestRepository.save(request);

        // Notify the requester that their request has been rejected.
        emailService.sendRequestRejectedEmail(savedRequest);

        return savedRequest;
    }

    /**
     * Only the acting user's own department admin may approve/reject a request,
     * and only for requests raised in the department they administer.
     */
    private void validateIsDepartmentAdmin(PurchaseRequest request, Long approverId) {
        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + approverId));

        if (approver.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only a department admin can approve or reject purchase requests");
        }

        Department department = request.getDepartment();
        if (department.getAdmin() == null || !department.getAdmin().getId().equals(approverId)) {
            throw new ForbiddenException("You are not the admin of the '" + department.getName() + "' department");
        }
    }

    @Override
    public PurchaseRequest getPurchaseRequestById(Long id) {
        return purchaseRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase request not found with id: " + id));
    }

    @Override
    public List<PurchaseRequest> getAllPurchaseRequests() {
        return purchaseRequestRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportAdminProcurementReportAsCsv() {
        List<PurchaseRequest> requests = purchaseRequestRepository.findAll();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (PrintWriter writer = new PrintWriter(out, true, StandardCharsets.UTF_8)) {
            writer.println("Request ID,User ID,User Name,User Email,Department,Request Title,Products,Amount,Approval Status,Payment Status,Payment Amount,Supplier,Details Sent At,Paid At,Created At");

            for (PurchaseRequest request : requests) {
                Payment payment = paymentRepository.findByPurchaseRequestId(request.getId()).orElse(null);
                String paymentStatus = getReportPaymentStatus(request, payment);
                String products = request.getItems().stream()
                        .map(item -> item.getProduct().getName() + " x" + item.getQuantity())
                        .reduce((a, b) -> a + "; " + b)
                        .orElse("");

                String userId = request.getRequester() != null && request.getRequester().getId() != null
                        ? String.valueOf(request.getRequester().getId()) : "";
                String userName = request.getRequester() != null ? request.getRequester().getFullName() : "";
                String userEmail = request.getRequester() != null ? request.getRequester().getEmail() : "";
                String department = request.getDepartment() != null ? request.getDepartment().getName() : "";
                String supplier = payment != null && payment.getSupplier() != null ? payment.getSupplier().getName() : "";
                String paymentAmount = payment != null && payment.getAmount() != null ? payment.getAmount().toString() : "";
                String detailsSentAt = payment != null && payment.getDetailsSentAt() != null ? payment.getDetailsSentAt().toString() : "";
                String paidAt = payment != null && payment.getPaidAt() != null ? payment.getPaidAt().toString() : "";
                String createdAt = request.getCreatedAt() != null ? request.getCreatedAt().toString() : "";

                writer.println(String.join(",",
                        csv(String.valueOf(request.getId())),
                        csv(userId),
                        csv(userName),
                        csv(userEmail),
                        csv(department),
                        csv(request.getTitle()),
                        csv(products),
                        csv(request.getTotalAmount() != null ? request.getTotalAmount().toString() : ""),
                        csv(request.getStatus() != null ? request.getStatus().name() : ""),
                        csv(paymentStatus),
                        csv(paymentAmount),
                        csv(supplier),
                        csv(detailsSentAt),
                        csv(paidAt),
                        csv(createdAt)
                ));
            }
        }
        return out.toByteArray();
    }

    private String getReportPaymentStatus(PurchaseRequest request, Payment payment) {
        if (request.getStatus() == RequestStatus.REJECTED) {
            return "NOT_APPLICABLE";
        }
        if (request.getStatus() != RequestStatus.APPROVED) {
            return "NOT_READY";
        }
        if (payment == null) {
            return "PAYMENT_PENDING";
        }
        if (payment.getStatus() == PaymentStatus.PAID) {
            return "PAID";
        }
        if (payment.getStatus() == PaymentStatus.DETAILS_SENT) {
            return "PAYMENT_PENDING";
        }
        return "PAYMENT_PENDING";
    }

    private String csv(String value) {
        if (value == null) return "";
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    @Override
    public List<PurchaseRequest> getPurchaseRequestsByRequester(Long requesterId) {
        return purchaseRequestRepository.findByRequesterId(requesterId);
    }

    @Override
    public List<PurchaseRequest> getPurchaseRequestsByApprover(Long approverId) {
        return purchaseRequestRepository.findByCurrentApproverId(approverId);
    }

    @Override
    public List<PurchaseRequest> getPurchaseRequestsByDepartment(Long departmentId, Long requestingUserId) {
        User requestingUser = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + requestingUserId));

        boolean isDeptAdmin = requestingUser.getRole() == Role.ADMIN
                && requestingUser.getDepartment() != null
                && requestingUser.getDepartment().getId().equals(departmentId);

        if (!isDeptAdmin) {
            throw new ForbiddenException("Only that department's admin can view its purchase requests");
        }

        return purchaseRequestRepository.findByDepartmentId(departmentId);
    }

    @Override
    @Transactional
    public PurchaseRequest updatePurchaseRequest(Long id, PurchaseRequest requestDetails, Long userId) {
        PurchaseRequest request = getPurchaseRequestById(id);

        if (!request.getRequester().getId().equals(userId)) {
            throw new BadRequestException("Only the requester can modify this purchase request");
        }

        if (request.getStatus() != RequestStatus.DRAFT) {
            throw new BadRequestException("Only DRAFT purchase requests can be updated");
        }

        request.setTitle(requestDetails.getTitle());
        request.setDescription(requestDetails.getDescription());

        // Reset items
        request.getItems().clear();
        BigDecimal total = BigDecimal.ZERO;
        for (PurchaseRequest.PurchaseRequestItem item : requestDetails.getItems()) {
            Product product = productRepository.findById(item.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + item.getProduct().getId()));
            
            PurchaseRequest.PurchaseRequestItem newItem = new PurchaseRequest.PurchaseRequestItem();
            newItem.setProduct(product);
            newItem.setQuantity(item.getQuantity());
            newItem.setPrice(product.getPrice());

            BigDecimal lineTotal = product.getPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()));

            newItem.setTotalPrice(lineTotal);

            total = total.add(lineTotal);
            request.getItems().add(newItem);
            
        }
        request.setTotalAmount(total);

        return purchaseRequestRepository.save(request);
    }

    @Override
    @Transactional
    public void deletePurchaseRequest(Long id, Long userId) {
        PurchaseRequest request = getPurchaseRequestById(id);

        if (!request.getRequester().getId().equals(userId)) {
            throw new BadRequestException("Only the requester can delete this purchase request");
        }

        if (request.getStatus() != RequestStatus.DRAFT) {
            throw new BadRequestException("Only DRAFT purchase requests can be deleted");
        }

        purchaseRequestRepository.delete(request);
    }
}
