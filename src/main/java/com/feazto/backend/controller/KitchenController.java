package com.feazto.backend.controller;

import com.feazto.backend.model.Dish;
import com.feazto.backend.model.Kitchen;
import com.feazto.backend.repository.DishRepository;
import com.feazto.backend.repository.KitchenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/kitchens")
@CrossOrigin(origins = "*")
public class KitchenController {

    @Autowired
    private KitchenRepository kitchenRepository;

    @Autowired
    private DishRepository dishRepository;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getKitchens(@RequestParam(required = false) Boolean featured) {
        List<Kitchen> kitchens;
        if (featured != null) {
            kitchens = kitchenRepository.findByFeatured(featured);
        } else {
            kitchens = kitchenRepository.findAll();
        }

        List<Map<String, Object>> response = kitchens.stream().map(k -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", k.getId().toString());
            map.put("name", k.getName());
            map.put("cuisine", k.getCuisine());
            map.put("rating", k.getRating());
            map.put("ratingCount", k.getRatingCount());
            map.put("story", k.getStory());
            map.put("image", k.getImage());
            map.put("since", k.getSince());
            map.put("tag", k.getTag());
            map.put("time", k.getDeliveryTime());
            map.put("price", k.getPriceForTwo());
            map.put("featured", k.isFeatured());
            map.put("sub", k.getCuisine());
            map.put("title", k.getName());
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getKitchenById(@PathVariable UUID id) {
        Optional<Kitchen> kitchenOpt = kitchenRepository.findById(id);
        if (kitchenOpt.isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Kitchen not found");
            return ResponseEntity.status(404).body(error);
        }

        Kitchen k = kitchenOpt.get();
        Map<String, Object> map = new HashMap<>();
        map.put("id", k.getId().toString());
        map.put("name", k.getName());
        map.put("cuisine", k.getCuisine());
        map.put("rating", k.getRating());
        map.put("ratingCount", k.getRatingCount());
        map.put("story", k.getStory());
        map.put("image", k.getImage());
        map.put("since", k.getSince());
        map.put("tag", k.getTag());
        map.put("time", k.getDeliveryTime());
        map.put("price", k.getPriceForTwo());
        map.put("title", k.getName());

        return ResponseEntity.ok(map);
    }

    @GetMapping("/{id}/menu")
    public ResponseEntity<List<Map<String, Object>>> getKitchenMenu(@PathVariable UUID id) {
        List<Dish> dishes = dishRepository.findByKitchenId(id);

        List<Map<String, Object>> response = dishes.stream().map(d -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", d.getId().toString());
            map.put("kitchenId", d.getKitchen().getId().toString());
            map.put("name", d.getName());
            map.put("desc", d.getDescription());
            map.put("price", d.getPrice());
            map.put("image", d.getImage());
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }
}
