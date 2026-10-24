package vn.edu.demo.orderservice.domain;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record OrderRequest(
        @NotBlank String userId,
        @NotBlank String status,
        @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal total) {
}
