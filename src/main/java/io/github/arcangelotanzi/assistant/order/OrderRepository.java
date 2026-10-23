package io.github.arcangelotanzi.assistant.order;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    @Query("""
            select o from Order o
            where o.status not in(OrderStatus.DELIVERED,OrderStatus.CANCELLED)
            and o.promisedDeliveryDate < :today
            order by o.promisedDeliveryDate
            """)
    // Order.customer is LAZY: without a graph, each getCustomer() access on the results
    // triggers one extra query (N+1). The graph fetches customers in the same query via a join.
    // Verified by OrderRepositoryTest#findLateOrdersLoadsCustomersInASingleQuery (1 query vs 4).
    @EntityGraph(attributePaths = "customer")
    List<Order> findLateOrders(@Param("today") LocalDate today);
}
