package com.feazto.backend.model;

import jakarta.persistence.*;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "dishes")
public class Dish {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kitchen_id", nullable = false)
    private Kitchen kitchen;

    @Column(nullable = false)
    private String name;

    @Column(name = "description", nullable = false)
    @JsonProperty("desc")
    private String description;

    @Column(nullable = false)
    private String price;

    @Column(nullable = false)
    private String image;

    public Dish() {}

    public Dish(UUID id, Kitchen kitchen, String name, String description, String price, String image) {
        this.id = id;
        this.kitchen = kitchen;
        this.name = name;
        this.description = description;
        this.price = price;
        this.image = image;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Kitchen getKitchen() {
        return kitchen;
    }

    public void setKitchen(Kitchen kitchen) {
        this.kitchen = kitchen;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
}
