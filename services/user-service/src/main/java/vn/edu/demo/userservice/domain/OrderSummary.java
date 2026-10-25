package vn.edu.demo.userservice.domain;

import java.math.BigDecimal;

public record OrderSummary(String id, String userId, String status, BigDecimal total) {
}
