package com.groceryshop.service;

import com.groceryshop.dto.CategoryDTO;
import com.groceryshop.entity.Category;
import com.groceryshop.exception.BadRequestException;
import com.groceryshop.exception.ResourceNotFoundException;
import com.groceryshop.mapper.EntityMapper;
import com.groceryshop.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    public List<CategoryDTO> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(EntityMapper::toCategoryDTO)
                .collect(Collectors.toList());
    }

    public List<CategoryDTO> getActiveCategories() {
        return categoryRepository.findByIsActiveTrue().stream()
                .map(EntityMapper::toCategoryDTO)
                .collect(Collectors.toList());
    }

    public CategoryDTO getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục với id: " + id));
        return EntityMapper.toCategoryDTO(category);
    }

    @Transactional
    public CategoryDTO createCategory(CategoryDTO dto) {
        if (categoryRepository.findByName(dto.getName()).isPresent()) {
            throw new BadRequestException("Tên danh mục đã tồn tại");
        }
        Category category = EntityMapper.toCategoryEntity(dto);
        if (category.getIsActive() == null) category.setIsActive(true);
        Category saved = categoryRepository.save(category);
        return EntityMapper.toCategoryDTO(saved);
    }

    @Transactional
    public CategoryDTO updateCategory(Long id, CategoryDTO dto) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục với id: " + id));
        categoryRepository.findByName(dto.getName())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(id)) {
                        throw new BadRequestException("Tên danh mục đã được sử dụng");
                    }
                });

        category.setName(dto.getName());
        category.setDescription(dto.getDescription());
        if (dto.getImage() != null) {
            category.setImage(dto.getImage());
        }
        if (dto.getIsActive() != null) {
            category.setIsActive(dto.getIsActive());
        }
        Category updated = categoryRepository.save(category);
        return EntityMapper.toCategoryDTO(updated);
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục với id: " + id));
        categoryRepository.delete(category);
    }
}
