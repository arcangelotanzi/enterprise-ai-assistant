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

    // the customer attribute in Order class is declared LAZY. It means that whenever you get an
    // order form the db, it never returns the customer associated.
    // For retrieve the customer, you need the annotation @EntityGraph. In this way you are saying:
    // for this query I want the db retrieve also the customer information.
    // This annotation turns into a join into the query.
    @EntityGraph(attributePaths = "customer")
    List<Order> findLateOrders(@Param("today") LocalDate today);
}
