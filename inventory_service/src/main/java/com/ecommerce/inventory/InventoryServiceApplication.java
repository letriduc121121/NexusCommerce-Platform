package com.ecommerce.inventory;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.EnableKafka;
import com.ecommerce.inventory.repository.InventoryRepository;

@EnableKafka
@SpringBootApplication
public class InventoryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryServiceApplication.class, args);
    }

    @Bean
    public CommandLineRunner resetAllStockTo100(InventoryRepository inventoryRepository) {
        return args -> {
            try {
                inventoryRepository.findAll().forEach(inv -> {
                    inv.setQuantity(100);
                    inv.setReservedQuantity(0);
                    inv.setActive(true);
                    inventoryRepository.save(inv);
                });
                System.out.println("✅ Successfully updated all inventory records stock quantity to 100!");
            } catch (Exception e) {
                System.err.println("⚠️ Could not update inventory stock on startup: " + e.getMessage());
            }
        };
    }
}
