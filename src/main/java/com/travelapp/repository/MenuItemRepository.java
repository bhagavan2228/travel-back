package com.travelapp.repository;

import com.travelapp.entity.MenuItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
    Page<MenuItem> findByRestaurantIdOrderBySortOrderAsc(Long restaurantId, Pageable pageable);
    long countByRestaurantId(Long restaurantId);
    boolean existsByRestaurantId(Long restaurantId);
}
