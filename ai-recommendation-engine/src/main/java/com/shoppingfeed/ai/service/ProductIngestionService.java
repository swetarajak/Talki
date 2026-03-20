// ai-recommendation-engine/src/main/java/com/shoppingfeed/ai/service/ProductIngestionService.java
package com.shoppingfeed.ai.service;

import com.shoppingfeed.ai.model.Product;
import com.shoppingfeed.ai.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductIngestionService {

    private final ProductRepository productRepository;
    private final VectorStore vectorStore;

    /**
     * @EventListener(ApplicationReadyEvent) — runs this method
     * automatically when the Spring application has fully started.
     *
     * We use it to seed sample products on first run.
     * In production, products would be added via an admin API.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void ingestSampleProducts() {

        // Only seed if database is empty
        if (productRepository.count() > 0) {
            log.info("Products already exist, skipping ingestion");
            return;
        }

        log.info("Seeding sample products into database and vector store...");

        List<Product> products = createSampleProducts();
        productRepository.saveAll(products);
        log.info("Saved {} products to PostgreSQL", products.size());

        // Now create embeddings and store in pgvector
        ingestIntoVectorStore(products);
        log.info("Ingested {} products into vector store", products.size());
    }

    /**
     * Converts products to Documents and stores them in pgvector.
     *
     * A Document in Spring AI has:
     *   - content: the text that gets embedded (converted to vector)
     *   - metadata: extra data stored alongside the vector
     *               (used to retrieve full product details later)
     */
    public void ingestIntoVectorStore(List<Product> products) {
        List<Document> documents = new ArrayList<>();

        for (Product product : products) {
            Document doc = new Document(
                    // The text that gets converted to an embedding vector
                    product.toEmbeddingText(),

                    // Metadata — stored with the vector, returned in search results
                    Map.of(
                            "productId",  product.getId(),
                            "name",       product.getName(),
                            "category",   product.getCategory(),
                            "price",      product.getPrice().toString(),
                            "brand",      product.getBrand(),
                            "rating",     String.valueOf(product.getAverageRating())
                    )
            );
            documents.add(doc);
        }

        // This call: converts each document's content to a vector
        // using OpenAI embeddings API, then stores in pgvector
        vectorStore.add(documents);
    }

    /**
     * Sample products for testing.
     * In production these come from your product catalog database.
     */
    private List<Product> createSampleProducts() {
        return List.of(
                Product.builder()
                        .name("Floral Maxi Dress")
                        .description("Beautiful floral print maxi dress perfect for summer. " +
                                "Made from breathable cotton. Ideal for casual outings.")
                        .price(new BigDecimal("1299"))
                        .category("dresses")
                        .brand("FabIndia")
                        .tags("floral,summer,casual,cotton,maxi")
                        .averageRating(4.5)
                        .reviewCount(234)
                        .imageUrl("https://example.com/dress1.jpg")
                        .build(),

                Product.builder()
                        .name("Cotton Anarkali Kurta")
                        .description("Elegant Anarkali style kurta in soft cotton fabric. " +
                                "Traditional Indian design with modern touch. " +
                                "Perfect for festivals and casual wear.")
                        .price(new BigDecimal("899"))
                        .category("kurtas")
                        .brand("FabIndia")
                        .tags("anarkali,kurta,cotton,traditional,ethnic")
                        .averageRating(4.7)
                        .reviewCount(456)
                        .imageUrl("https://example.com/kurta1.jpg")
                        .build(),

                Product.builder()
                        .name("Palazzo Set with Dupatta")
                        .description("Stylish palazzo pants with matching top and dupatta. " +
                                "Comfortable and trendy. Great for office and parties.")
                        .price(new BigDecimal("1599"))
                        .category("sets")
                        .brand("W for Woman")
                        .tags("palazzo,set,dupatta,ethnic,comfortable")
                        .averageRating(4.3)
                        .reviewCount(178)
                        .imageUrl("https://example.com/palazzo1.jpg")
                        .build(),

                Product.builder()
                        .name("Printed Wrap Dress")
                        .description("Trendy wrap style dress with geometric print. " +
                                "Versatile — can be dressed up or down. " +
                                "Available in multiple colors.")
                        .price(new BigDecimal("1099"))
                        .category("dresses")
                        .brand("Zara")
                        .tags("wrap,geometric,print,versatile,trendy")
                        .averageRating(4.2)
                        .reviewCount(312)
                        .imageUrl("https://example.com/dress2.jpg")
                        .build(),

                Product.builder()
                        .name("Embroidered Straight Kurta")
                        .description("Beautiful embroidered straight cut kurta. " +
                                "Hand embroidery on neckline. " +
                                "Perfect for festive occasions.")
                        .price(new BigDecimal("1799"))
                        .category("kurtas")
                        .brand("Biba")
                        .tags("embroidered,straight,kurta,festive,handwork")
                        .averageRating(4.8)
                        .reviewCount(567)
                        .imageUrl("https://example.com/kurta2.jpg")
                        .build(),

                Product.builder()
                        .name("Casual Linen Shirt Dress")
                        .description("Relaxed linen shirt dress for everyday wear. " +
                                "Breathable fabric, perfect for hot weather. " +
                                "Minimalist design.")
                        .price(new BigDecimal("799"))
                        .category("dresses")
                        .brand("H&M")
                        .tags("linen,casual,shirt-dress,minimalist,everyday")
                        .averageRating(4.1)
                        .reviewCount(145)
                        .imageUrl("https://example.com/dress3.jpg")
                        .build()
        );
    }
}