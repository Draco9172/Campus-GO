package com.campusgo.repository;

import com.campusgo.entity.Order;
import com.campusgo.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.OffsetDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // Vendor order queue sorted by oldest first
    List<Order> findByVendorIdAndStatusOrderByPlacedAtAsc(Long vendorId, OrderStatus status);

    // Student order history sorted by newest first
    List<Order> findByStudentIdOrderByPlacedAtDesc(Long studentId);

    // For enforcing unpaid order cap (max 2-3 active orders)
    long countByStudentIdAndStatusIn(Long studentId, List<OrderStatus> statuses);

    // For the auto-cancel scheduler (find ACCEPTED orders older than 5 mins)
    List<Order> findByStatusAndAcceptedAtBefore(OrderStatus status, OffsetDateTime threshold);
}
