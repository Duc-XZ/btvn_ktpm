package vn.edu.demo.orderservice.api;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import vn.edu.demo.orderservice.domain.Order;
import vn.edu.demo.orderservice.domain.OrderRequest;
import vn.edu.demo.orderservice.domain.OrderStore;

@RestController
@RequestMapping("/orders")
public class OrderController {
    private final OrderStore orders;

    public OrderController(OrderStore orders) {
        this.orders = orders;
    }

    @GetMapping
    public List<Order> list(@RequestParam(required = false) String userId) {
        return orders.findAll(userId);
    }

    @GetMapping("/{orderId}")
    public Order get(@PathVariable String orderId) {
        return orders.findById(orderId);
    }

    @PostMapping
    public ResponseEntity<Order> create(@Valid @RequestBody OrderRequest request) {
        Order created = orders.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{orderId}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{orderId}")
    public Order update(@PathVariable String orderId, @Valid @RequestBody OrderRequest request) {
        return orders.update(orderId, request);
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> delete(@PathVariable String orderId) {
        orders.delete(orderId);
        return ResponseEntity.noContent().build();
    }
}
