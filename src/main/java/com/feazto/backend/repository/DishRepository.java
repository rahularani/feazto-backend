package com.feazto.backend.repository;

import com.feazto.backend.model.Dish;
import com.feazto.backend.model.Kitchen;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface DishRepository extends JpaRepository<Dish, UUID> {
    List<Dish> findByKitchen(Kitchen kitchen);
    List<Dish> findByKitchenId(UUID kitchenId);
}
