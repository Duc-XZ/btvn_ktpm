package vn.edu.demo.orderservice.domain;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class OrderStore {
    private final ConcurrentMap<String, Order> orders = new ConcurrentHashMap<>();

    public OrderStore() {
        orders.put("o-456", new Order("o-456", "u-123", "NEW", new BigDecimal("125000.00")));
        orders.put("o-789", new Order("o-789", "u-123", "PAID", new BigDecimal("89000.00")));
        orders.put("o-901", new Order("o-901", "u-456", "SHIPPED", new BigDecimal("249000.00")));
    }

    public List<Order> findAll(String userId) {
        return orders.values().stream()
                .filter(order -> userId == null || order.userId().equals(userId))
                .sorted(Comparator.comparing(Order::id))
                .toList();
    }

    public Order findById(String id) {
        Order order = orders.get(id);
        if (order == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
        }
        return order;
    }

    public Order create(OrderRequest request) {
        String id = UUID.randomUUID().toString();
        Order order = new Order(id, request.userId(), request.status(), request.total());
        orders.put(id, order);
        return order;
    }

    public Order update(String id, OrderRequest request) {
        if (!orders.containsKey(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
        }
        Order updated = new Order(id, request.userId(), request.status(), request.total());
        orders.put(id, updated);
        return updated;
    }

    public void delete(String id) {
        if (orders.remove(id) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
        }
    }
}
