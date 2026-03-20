// ai-recommendation-engine/src/main/java/com/shoppingfeed/ai/model/Product.java
package com.shoppingfeed.ai.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * @Entity — tells JPA this class maps to a database table
 * @Table  — specifies the table name
 *
 * Spring will auto-create this table on startup
 * because of spring.jpa.hibernate.ddl-auto=update in application.yml
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    private BigDecimal price;
    private String category;
    private String brand;
    private double averageRating;
    private int reviewCount;

    /**
     * Tags stored as a comma-separated string in DB
     * e.g. "floral,summer,casual,cotton"
     *
     * @ElementCollection would be cleaner but this is
     * simpler for our learning project
     */
    private String tags;

    // Image URL for the frontend to display
    private String imageUrl;

    // Whether this product is active/available
    @Builder.Default
    private boolean active = true;

    /**
     * Helper to convert tags string to list
     */
    @Transient  // not stored in DB — computed on the fly
    public List<String> getTagList() {
        if (tags == null || tags.isEmpty()) return List.of();
        return List.of(tags.split(","));
    }

    /**
     * Creates a rich text description for embedding.
     * This is what gets converted to a vector and stored in pgvector.
     *
     * The richer the description, the better the semantic search.
     */
    @Transient
    public String toEmbeddingText() {
        return String.format(
                "Product: %s. Category: %s. Brand: %s. " +
                        "Description: %s. Tags: %s. Price: %s. Rating: %.1f",
                name, category, brand, description, tags, price, averageRating
        );
    }
}