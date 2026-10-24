package vn.edu.demo.orderservice.domain;

import java.math.BigDecimal;

public record Order(String id, String userId, String status, BigDecimal total) {
}
