package com.infosys.procurementsystem.service;

import com.infosys.procurementsystem.entity.Supplier;
import java.util.List;

public interface SupplierService {
    Supplier createSupplier(Supplier supplier);
    Supplier getSupplierById(Long id);
    List<Supplier> getAllSuppliers();
    Supplier updateSupplier(Long id, Supplier supplierDetails);
    void deleteSupplier(Long id);
}
