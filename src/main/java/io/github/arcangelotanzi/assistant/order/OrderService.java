package io.github.arcangelotanzi.assistant.order;

import io.github.arcangelotanzi.assistant.customer.Customer;
import io.github.arcangelotanzi.assistant.customer.CustomerNotFoundException;
import io.github.arcangelotanzi.assistant.customer.CustomerRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final Clock clock;

    @Autowired
    public OrderService(OrderRepository orderRepository, CustomerRepository customerRepository, Clock clock) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.clock = clock;
    }

    public List<LateOrderResponse> findLateOrders() {
        LocalDate today = LocalDate.now(clock);
        List<Order> lateOrders = orderRepository.findLateOrders(today);
        return lateOrders.stream()
                .map(order -> LateOrderResponse.from(order, today))
                .toList();
    }

    public OrderResponse findById(Long id) {
        return orderRepository.findById(id).map(OrderResponse::from).orElseThrow(() -> new OrderNotFoundException(id));
    }

    @Transactional
    public OrderResponse create(CreateOrderRequest request) {
        LocalDate today = LocalDate.now(clock);
        if (request.promisedDeliveryDate().isBefore(today)) {
            throw new InvalidDeliveryDateException(request.promisedDeliveryDate(), today);
        }

        Customer customer = customerRepository
                .findById(request.customerId())
                .orElseThrow(() -> new CustomerNotFoundException(request.customerId()));

        Order newOrder = new Order(customer, today, request.promisedDeliveryDate(), request.totalAmount());

        Order saved = orderRepository.save(newOrder);
        return OrderResponse.from(saved);
    }
}
