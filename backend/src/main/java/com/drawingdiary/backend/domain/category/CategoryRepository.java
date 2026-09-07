package com.drawingdiary.backend.domain.category;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByUserIdOrderByIdAsc(Long userId);

    boolean existsByUserIdAndName(Long userId, String name);
}
