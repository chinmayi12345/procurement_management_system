package com.infosys.procurementsystem.service.impl;

import com.infosys.procurementsystem.entity.Category;
import com.infosys.procurementsystem.exception.BadRequestException;
import com.infosys.procurementsystem.exception.ResourceNotFoundException;
import com.infosys.procurementsystem.repository.CategoryRepository;
import com.infosys.procurementsystem.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public Category createCategory(Category category) {
        if (categoryRepository.findByName(category.getName()).isPresent()) {
            throw new BadRequestException("Category name already exists");
        }
        return categoryRepository.save(category);
    }

    @Override
    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
    }

    @Override
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Override
    @Transactional
    public Category updateCategory(Long id, Category categoryDetails) {
        Category category = getCategoryById(id);
        
        categoryRepository.findByName(categoryDetails.getName()).ifPresent(c -> {
            if (!c.getId().equals(id)) {
                throw new BadRequestException("Category name already exists");
            }
        });

        category.setName(categoryDetails.getName());
        category.setDescription(categoryDetails.getDescription());
        return categoryRepository.save(category);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        Category category = getCategoryById(id);
        categoryRepository.delete(category);
    }
}
