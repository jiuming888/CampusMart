package com.campusmart.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductDTO {
    private String name;
    private String description;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private String images;
    private Long categoryId;
    private String conditionLevel;
    private String location;
    private Integer stock;
}
