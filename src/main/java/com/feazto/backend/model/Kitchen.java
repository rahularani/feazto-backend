package com.feazto.backend.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "kitchens")
public class Kitchen {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String cuisine;

    @Column(nullable = false)
    private String rating;

    @Column(name = "rating_count", nullable = false)
    private String ratingCount;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String story;

    @Column(nullable = false)
    private String image;

    @Column(nullable = false)
    private String since;

    @Column(nullable = false)
    private String tag = "Open";

    @Column(name = "delivery_time", nullable = false)
    private String deliveryTime = "30-45 min";

    @Column(name = "price_for_two", nullable = false)
    private String priceForTwo = "₹300";

    private boolean featured = false;

    public Kitchen() {}

    public Kitchen(UUID id, String name, String cuisine, String rating, String ratingCount, String story, String image, String since, String tag, String deliveryTime, String priceForTwo, boolean featured) {
        this.id = id;
        this.name = name;
        this.cuisine = cuisine;
        this.rating = rating;
        this.ratingCount = ratingCount;
        this.story = story;
        this.image = image;
        this.since = since;
        this.tag = tag;
        this.deliveryTime = deliveryTime;
        this.priceForTwo = priceForTwo;
        this.featured = featured;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCuisine() {
        return cuisine;
    }

    public void setCuisine(String cuisine) {
        this.cuisine = cuisine;
    }

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }

    public String getRatingCount() {
        return ratingCount;
    }

    public void setRatingCount(String ratingCount) {
        this.ratingCount = ratingCount;
    }

    public String getStory() {
        return story;
    }

    public void setStory(String story) {
        this.story = story;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getSince() {
        return since;
    }

    public void setSince(String since) {
        this.since = since;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getDeliveryTime() {
        return deliveryTime;
    }

    public void setDeliveryTime(String deliveryTime) {
        this.deliveryTime = deliveryTime;
    }

    public String getPriceForTwo() {
        return priceForTwo;
    }

    public void setPriceForTwo(String priceForTwo) {
        this.priceForTwo = priceForTwo;
    }

    public boolean isFeatured() {
        return featured;
    }

    public void setFeatured(boolean featured) {
        this.featured = featured;
    }
}
