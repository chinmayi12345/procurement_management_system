package com.infosys.procurementsystem.service.impl;

import com.infosys.procurementsystem.entity.Category;
import com.infosys.procurementsystem.entity.Product;
import com.infosys.procurementsystem.entity.Supplier;
import com.infosys.procurementsystem.exception.BadRequestException;
import com.infosys.procurementsystem.exception.ResourceNotFoundException;
import com.infosys.procurementsystem.repository.CategoryRepository;
import com.infosys.procurementsystem.repository.ProductRepository;
import com.infosys.procurementsystem.repository.SupplierRepository;
import com.infosys.procurementsystem.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;

    @Override
    @Transactional
    public Product createProduct(Product product) {
        if (product.getAvailableStock() == null || product.getAvailableStock() < 0) {
            throw new BadRequestException("Available stock cannot be negative");
        }
        if (productRepository.findBySku(product.getSku()).isPresent()) {
            throw new BadRequestException("Product SKU already exists");
        }
        
        Category category = categoryRepository.findById(product.getCategory().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + product.getCategory().getId()));
        
        Supplier supplier = supplierRepository.findById(product.getSupplier().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + product.getSupplier().getId()));
        
        product.setCategory(category);
        product.setSupplier(supplier);
        if (product.getStatus() == null) {
            product.setStatus(product.getAvailableStock() > 0 ? com.infosys.procurementsystem.enums.ProductStatus.ACTIVE : com.infosys.procurementsystem.enums.ProductStatus.OUT_OF_STOCK);
        }
        return productRepository.save(product);
    }

    @Override
    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    @Override
    public Product getProductBySku(String sku) {
        return productRepository.findBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with SKU: " + sku));
    }

    @Override
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Override
    @Transactional
    public Product updateProduct(Long id, Product productDetails) {
        Product product = getProductById(id);

        productRepository.findBySku(productDetails.getSku()).ifPresent(p -> {
            if (!p.getId().equals(id)) {
                throw new BadRequestException("Product SKU already exists");
            }
        });

        Category category = categoryRepository.findById(productDetails.getCategory().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + productDetails.getCategory().getId()));
        
        Supplier supplier = supplierRepository.findById(productDetails.getSupplier().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + productDetails.getSupplier().getId()));

        product.setName(productDetails.getName());
        product.setDescription(productDetails.getDescription());
        product.setPrice(productDetails.getPrice());
        product.setSku(productDetails.getSku());
        product.setStatus(productDetails.getStatus());
        product.setAvailableStock(productDetails.getAvailableStock() == null ? 0 : productDetails.getAvailableStock());
        product.setImageData(productDetails.getImageData());
        if (product.getAvailableStock() <= 0 && product.getStatus() == com.infosys.procurementsystem.enums.ProductStatus.ACTIVE) {
            product.setStatus(com.infosys.procurementsystem.enums.ProductStatus.OUT_OF_STOCK);
        } else if (product.getAvailableStock() > 0 && product.getStatus() == com.infosys.procurementsystem.enums.ProductStatus.OUT_OF_STOCK) {
            product.setStatus(com.infosys.procurementsystem.enums.ProductStatus.ACTIVE);
        }
        product.setCategory(category);
        product.setSupplier(supplier);
        
        return productRepository.save(product);
    }

    @Override
    @Transactional
    public Product updateStock(Long id, Integer quantity) {
        if (quantity == null || quantity < 0) {
            throw new BadRequestException("Stock quantity cannot be negative");
        }
        Product product = getProductById(id);
        product.setAvailableStock(quantity);
        product.setStatus(quantity > 0 ? com.infosys.procurementsystem.enums.ProductStatus.ACTIVE : com.infosys.procurementsystem.enums.ProductStatus.OUT_OF_STOCK);
        return productRepository.save(product);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        Product product = getProductById(id);
        productRepository.delete(product);
    }
}
