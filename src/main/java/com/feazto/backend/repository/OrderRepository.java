package com.feazto.backend.repository;

import com.feazto.backend.model.Order;
import com.feazto.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findByUserOrderByCreatedAtDesc(User user);
}
