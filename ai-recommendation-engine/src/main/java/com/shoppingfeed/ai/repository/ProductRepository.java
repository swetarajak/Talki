// ai-recommendation-engine/src/main/java/com/shoppingfeed/ai/repository/ProductRepository.java
package com.shoppingfeed.ai.repository;

import com.shoppingfeed.ai.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {

    // Find all active products
    List<Product> findByActiveTrue();

    // Find by category
    List<Product> findByCategoryAndActiveTrue(String category);

    // Find by IDs (used to fetch products after vector search)
    List<Product> findByIdIn(List<String> ids);

    // Search by name or description (simple text search)
    @Query("SELECT p FROM Product p WHERE " +
            "LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Product> searchByText(String query);
}