package com.feazto.backend.repository;

import com.feazto.backend.model.Kitchen;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface KitchenRepository extends JpaRepository<Kitchen, UUID> {
    List<Kitchen> findByFeatured(boolean featured);
}
