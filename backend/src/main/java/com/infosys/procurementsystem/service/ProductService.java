package com.infosys.procurementsystem.service;

import com.infosys.procurementsystem.entity.Product;
import java.util.List;

public interface ProductService {
    Product createProduct(Product product);
    Product getProductById(Long id);
    Product getProductBySku(String sku);
    List<Product> getAllProducts();
    Product updateProduct(Long id, Product productDetails);
    void deleteProduct(Long id);
    Product updateStock(Long id, Integer quantity);
}
