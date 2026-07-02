package com.feazto.backend.controller;

import com.feazto.backend.model.*;
import com.feazto.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private KitchenRepository kitchenRepository;

    @Autowired
    private DishRepository dishRepository;

    @Autowired
    private OrderRepository orderRepository;

    private User getAuthenticatedUser() {
        String userIdStr = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepository.findById(UUID.fromString(userIdStr)).orElseThrow(
                () -> new RuntimeException("Authenticated user not found")
        );
    }

    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> body) {
        try {
            User user = getAuthenticatedUser();
            UUID kitchenId = UUID.fromString((String) body.get("kitchenId"));
            String totalPrice = (String) body.get("totalPrice");
            String specialInstructions = (String) body.get("specialInstructions");

            Kitchen kitchen = kitchenRepository.findById(kitchenId).orElseThrow(
                    () -> new RuntimeException("Kitchen not found")
            );

            Order order = new Order();
            order.setUser(user);
            order.setKitchen(kitchen);
            order.setTotalPrice(totalPrice);
            order.setSpecialInstructions(specialInstructions);
            order.setStatus("PLACED");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> itemsList = (List<Map<String, Object>>) body.get("items");
            List<OrderItem> orderItems = new ArrayList<>();

            for (Map<String, Object> itemMap : itemsList) {
                UUID dishId = UUID.fromString((String) itemMap.get("dishId"));
                int quantity = ((Number) itemMap.get("quantity")).intValue();
                String price = (String) itemMap.get("price");

                Dish dish = dishRepository.findById(dishId).orElseThrow(
                        () -> new RuntimeException("Dish not found: " + dishId)
                );

                OrderItem orderItem = new OrderItem(order, dish, quantity, price);
                orderItems.add(orderItem);
            }

            order.setItems(orderItems);
            Order savedOrder = orderRepository.save(order);

            // Return custom mapped order
            return ResponseEntity.ok(mapOrderToResponse(savedOrder));
        } catch (Exception e) {
            Map<String, String> err = new HashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderById(@PathVariable UUID id) {
        Optional<Order> orderOpt = orderRepository.findById(id);
        if (orderOpt.isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Order not found");
            return ResponseEntity.status(404).body(error);
        }
        return ResponseEntity.ok(mapOrderToResponse(orderOpt.get()));
    }

    @GetMapping("/user")
    public ResponseEntity<?> getUserOrders() {
        try {
            User user = getAuthenticatedUser();
            List<Order> orders = orderRepository.findByUserOrderByCreatedAtDesc(user);
            List<Map<String, Object>> response = orders.stream()
                    .map(this::mapOrderToResponse)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> err = new HashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateOrderStatus(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        Optional<Order> orderOpt = orderRepository.findById(id);
        if (orderOpt.isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Order not found");
            return ResponseEntity.status(404).body(error);
        }

        Order order = orderOpt.get();
        String status = body.get("status");
        if (status != null) {
            order.setStatus(status);
            orderRepository.save(order);
        }
        return ResponseEntity.ok(mapOrderToResponse(order));
    }

    private Map<String, Object> mapOrderToResponse(Order order) {
        Map<String, Object> res = new HashMap<>();
        res.put("id", order.getId().toString());
        res.put("userId", order.getUser().getId().toString());
        res.put("kitchenId", order.getKitchen().getId().toString());
        res.put("kitchenName", order.getKitchen().getName());
        res.put("totalPrice", order.getTotalPrice());
        res.put("status", order.getStatus());
        res.put("specialInstructions", order.getSpecialInstructions());
        res.put("createdAt", order.getCreatedAt().toString());

        List<Map<String, Object>> items = order.getItems().stream().map(i -> {
            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("id", i.getId().toString());
            itemMap.put("dishId", i.getDish().getId().toString());
            itemMap.put("dishName", i.getDish().getName());
            itemMap.put("quantity", i.getQuantity());
            itemMap.put("price", i.getPrice());
            return itemMap;
        }).collect(Collectors.toList());

        res.put("items", items);
        return res;
    }
}
