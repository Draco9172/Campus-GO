package com.campusgo.repository;

import com.campusgo.entity.Vendor;
import com.campusgo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface VendorRepository extends JpaRepository<Vendor, Long> {
    Optional<Vendor> findByUser(User user);

    Optional<Vendor> findByUserId(Long userId);

    List<Vendor> findByIsOpenTrue();
}
