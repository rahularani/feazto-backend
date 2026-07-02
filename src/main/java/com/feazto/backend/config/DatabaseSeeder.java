package com.feazto.backend.config;

import com.feazto.backend.model.Dish;
import com.feazto.backend.model.Kitchen;
import com.feazto.backend.repository.DishRepository;
import com.feazto.backend.repository.KitchenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.UUID;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    @Autowired
    private KitchenRepository kitchenRepository;

    @Autowired
    private DishRepository dishRepository;

    @Override
    public void run(String... args) throws Exception {
        if (kitchenRepository.count() == 0) {
            System.out.println("Seeding Spring Boot PostgreSQL Database with initial Feazto data...");

            // Kitchen 1
            UUID k1Id = UUID.fromString("6d713c7a-9a99-4c55-b44c-cb14cf79435b");
            Kitchen k1 = new Kitchen(
                    k1Id,
                    "Amma's Kerala Kitchen",
                    "Authentic Syrian Christian recipes",
                    "4.9",
                    "120",
                    "Bringing 40 years of family recipes from Kottayam to Bangalore. Every dish is prepared exactly how I cook for my grandchildren, using spices sourced directly from our family farm in Kerala.",
                    "https://images.unsplash.com/photo-1596522868222-0df01d62b628?w=800&q=80",
                    "1985",
                    "Pre-order",
                    "45 mins",
                    "₹250 for two",
                    true
            );

            // Kitchen 2
            UUID k2Id = UUID.fromString("a1b63e80-77a8-4e12-8703-e847c1fb41ea");
            Kitchen k2 = new Kitchen(
                    k2Id,
                    "Sharma Ji's Rasoi",
                    "Pure veg North Indian thalis",
                    "4.8",
                    "85",
                    "Authentic North Indian food made with pure desi ghee, straight from the heart of Rajasthan. We believe food is worship.",
                    "https://images.unsplash.com/photo-1668236543090-82eba5ee5976?w=800&q=80",
                    "1998",
                    "Open",
                    "30 mins",
                    "₹320 for two",
                    true
            );

            // Kitchen 3
            UUID k3Id = UUID.fromString("c0df1b4a-fb7c-48c0-bc6f-2b7e19ad982b");
            Kitchen k3 = new Kitchen(
                    k3Id,
                    "Fatima's Biryani",
                    "Hyderabadi dum biryani specialists",
                    "4.7",
                    "210",
                    "Three generations of Hyderabadi biryani secrets, lovingly cooked in our family's original dum pot. Every grain of rice is cooked to perfection.",
                    "https://images.unsplash.com/photo-1547592180-85f173990554?w=800&q=80",
                    "1972",
                    "Pre-order",
                    "60 mins",
                    "₹380 for two",
                    true
            );

            kitchenRepository.saveAll(Arrays.asList(k1, k2, k3));

            // Dishes for Kitchen 1
            Dish d1_1 = new Dish(UUID.randomUUID(), k1, "Thalassery Biryani", "Jeerakasala rice cooked with tender chicken and authentic malabar spices.", "₹320", "https://images.unsplash.com/photo-1589302168068-964664d93dc0?w=300&q=80");
            Dish d1_2 = new Dish(UUID.randomUUID(), k1, "Appam & Stew", "Soft lace appams with rich coconut milk stew.", "₹220", "https://images.unsplash.com/photo-1542367592-8849eb950fd8?w=300&q=80");
            Dish d1_3 = new Dish(UUID.randomUUID(), k1, "Kerala Fish Curry", "Fresh catch cooked in tangy kodampuli and coconut gravy.", "₹280", "https://images.unsplash.com/photo-1596522868222-0df01d62b628?w=300&q=80");

            // Dishes for Kitchen 2
            Dish d2_1 = new Dish(UUID.randomUUID(), k2, "Special Thali", "Paneer butter masala, dal makhani, 2 rotis, rice, and gulab jamun.", "₹320", "https://images.unsplash.com/photo-1547592180-85f173990554?w=300&q=80");
            Dish d2_2 = new Dish(UUID.randomUUID(), k2, "Crispy Masala Dosa", "Golden crispy dosa with spiced potato filling.", "₹180", "https://images.unsplash.com/photo-1668236543090-82eba5ee5976?w=300&q=80");
            Dish d2_3 = new Dish(UUID.randomUUID(), k2, "Butter Chicken", "Tender chicken in rich, creamy tomato-butter gravy.", "₹350", "https://images.unsplash.com/photo-1547592180-85f173990554?w=300&q=80");

            // Dishes for Kitchen 3
            Dish d3_1 = new Dish(UUID.randomUUID(), k3, "Hyderabadi Dum Biryani", "Slow-cooked basmati with tender mutton and whole spices.", "₹420", "https://images.unsplash.com/photo-1547592180-85f173990554?w=300&q=80");
            Dish d3_2 = new Dish(UUID.randomUUID(), k3, "Chicken 65 Biryani", "Spicy fried chicken layered with fragrant basmati.", "₹380", "https://images.unsplash.com/photo-1547592180-85f173990554?w=300&q=80");
            Dish d3_3 = new Dish(UUID.randomUUID(), k3, "Veg Biryani", "Seasonal vegetables dum-cooked with saffron and fried onions.", "₹280", "https://images.unsplash.com/photo-1596522868222-0df01d62b628?w=300&q=80");

            dishRepository.saveAll(Arrays.asList(d1_1, d1_2, d1_3, d2_1, d2_2, d2_3, d3_1, d3_2, d3_3));

            System.out.println("Spring Boot data seeding complete!");
        }
    }
}
