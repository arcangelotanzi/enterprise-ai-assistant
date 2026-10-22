package io.github.arcangelotanzi.assistant.order;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final Clock clock;

    @Autowired
    public OrderService(OrderRepository orderRepository, Clock clock) {
        this.orderRepository = orderRepository;
        this.clock = clock;
    }

    public List<LateOrderResponse> findLateOrders() {
        LocalDate today = LocalDate.now(clock);
        List<Order> lateOrders = orderRepository.findLateOrders(today);
        return lateOrders.stream().map(order -> LateOrderResponse.from(order, today)).toList();
    }
}
