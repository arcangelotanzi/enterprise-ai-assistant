package io.github.arcangelotanzi.assistant.order;

import io.github.arcangelotanzi.assistant.TestcontainersConfiguration;
import io.github.arcangelotanzi.assistant.customer.Customer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestcontainersConfiguration.class)
class OrderRepositoryTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 21);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Customer customer;

    @BeforeEach
    void setUp() {
        customer = entityManager.persist(new Customer("Rossi S.r.l.", "rossi@test.example", null));
    }

    @Test
    void findLateOrdersReturnsOnlyOpenOrdersPromisedBeforeToday() {
        // Arrange: one order per case, plus a second late order that is older than the first
        Order lateYesterday = persist(orderPromisedOn(TODAY.minusDays(1)));
        Order lateTenDaysAgo = persist(orderPromisedOn(TODAY.minusDays(10)));
        persist(orderPromisedOn(TODAY));                 // due today: not late yet
        persist(orderPromisedOn(TODAY.plusDays(3)));     // future: not late

        Order delivered = orderPromisedOn(TODAY.minusDays(5));
        delivered.markDelivered(Instant.parse("2026-10-18T10:00:00Z"));
        persist(delivered);                              // closed: not late

        Order cancelled = orderPromisedOn(TODAY.minusDays(5));
        cancelled.cancel();
        persist(cancelled);                              // closed: not late

        entityManager.flush();
        entityManager.clear();

        // Act
        List<Order> result = orderRepository.findLateOrders(TODAY);

        // Assert: exactly the two late orders, oldest promise first
        assertThat(result)
                .extracting(Order::getId)
                .containsExactly(lateTenDaysAgo.getId(), lateYesterday.getId());
    }

    @Test
    void findLateOrdersLoadsTheCustomer() {
        persist(orderPromisedOn(TODAY.minusDays(1)));
        entityManager.flush();
        entityManager.clear();

        List<Order> result = orderRepository.findLateOrders(TODAY);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getCustomer().getName()).isEqualTo("Rossi S.r.l.");
    }

    @Test
    void findLateOrdersReturnsEmptyListWhenNothingIsLate() {
        persist(orderPromisedOn(TODAY.plusDays(1)));
        entityManager.flush();
        entityManager.clear();

        assertThat(orderRepository.findLateOrders(TODAY)).isEmpty();
    }

    private Order orderPromisedOn(LocalDate promisedDeliveryDate) {
        return new Order(customer, promisedDeliveryDate.minusDays(7), promisedDeliveryDate, new BigDecimal("100.00"));
    }

    private Order persist(Order order) {
        return entityManager.persist(order);
    }
}
