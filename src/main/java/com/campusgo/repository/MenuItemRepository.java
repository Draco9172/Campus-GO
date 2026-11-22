package com.campusgo.repository;

import com.campusgo.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
    List<MenuItem> findByVendorId(Long vendorId);

    List<MenuItem> findByVendorIdAndIsAvailableTrue(Long vendorId);
}
