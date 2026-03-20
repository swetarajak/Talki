package com.shoppingfeed.common.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {

    private String productId;
    private String name;
    private String description;
    private BigDecimal price;
    private String category;
    private List<String> tags;
    private String brand;
    private double averageRating;

}
