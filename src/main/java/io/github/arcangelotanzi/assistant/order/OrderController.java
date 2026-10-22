package io.github.arcangelotanzi.assistant.order;

import io.github.arcangelotanzi.assistant.customer.CreateCustomerRequest;
import io.github.arcangelotanzi.assistant.customer.CustomerResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    @Autowired
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/{id}")
    public OrderResponse findById(@PathVariable Long id) {
        return orderService.findById(id);
    }

    @GetMapping("/late")
    public List<LateOrderResponse> findLateOrders() {
        return orderService.findLateOrders();
    }

    @PostMapping
    public ResponseEntity<OrderResponse> create(
            @Valid
            @RequestBody
            CreateOrderRequest request) {
        OrderResponse orderCreated = orderService.create(request);
        URI uri = URI.create("/api/orders/" + orderCreated.id());
        return ResponseEntity.created(uri).body(orderCreated);
    }
}
