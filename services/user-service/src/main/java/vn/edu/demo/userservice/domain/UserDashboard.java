package vn.edu.demo.userservice.domain;

import java.util.List;

public record UserDashboard(User user, List<OrderSummary> orders) {
    public UserDashboard {
        orders = List.copyOf(orders);
    }
}
