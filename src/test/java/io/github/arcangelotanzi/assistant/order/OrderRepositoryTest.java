package io.github.arcangelotanzi.assistant.order;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.arcangelotanzi.assistant.TestcontainersConfiguration;
import io.github.arcangelotanzi.assistant.customer.Customer;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
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
        persist(orderPromisedOn(TODAY)); // due today: not late yet
        persist(orderPromisedOn(TODAY.plusDays(3))); // future: not late

        Order delivered = orderPromisedOn(TODAY.minusDays(5));
        delivered.markDelivered(Instant.parse("2026-10-18T10:00:00Z"));
        persist(delivered); // closed: not late

        Order cancelled = orderPromisedOn(TODAY.minusDays(5));
        cancelled.cancel();
        persist(cancelled); // closed: not late

        entityManager.flush();
        entityManager.clear();

        // Act
        List<Order> result = orderRepository.findLateOrders(TODAY);

        // Assert: exactly the two late orders, oldest promise first
        assertThat(result).extracting(Order::getId).containsExactly(lateTenDaysAgo.getId(), lateYesterday.getId());
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

    // "Open" is defined twice: in the @Query (NOT IN DELIVERED, CANCELLED) and in OrderStatus.isOpen().
    // This test fails as soon as the two diverge, also for statuses added in the future.
    @ParameterizedTest(name = "{0}")
    @EnumSource(OrderStatus.class)
    void findLateOrdersReturnsAPastDueOrderIfAndOnlyIfItsStatusIsOpen(OrderStatus status) {
        // Arrange: Order has no setters by design, so the row is written with native SQL.
        // delivered_at must be set iff status is DELIVERED (ck_orders_delivered_consistency).
        entityManager.flush(); // the customer row must exist before the native INSERT (FK)
        entityManager
                .getEntityManager()
                .createNativeQuery("""
                        insert into orders (customer_id, status, order_date, promised_delivery_date, delivered_at, total_amount)
                        values (:customerId, :status, :orderDate, :promisedDate,
                                case when :status = 'DELIVERED' then timestamptz '2026-10-18 10:00:00+00' end,
                                100.00)
                        """)
                .setParameter("customerId", customer.getId())
                .setParameter("status", status.name())
                .setParameter("orderDate", TODAY.minusDays(12))
                .setParameter("promisedDate", TODAY.minusDays(5))
                .executeUpdate();
        entityManager.clear();

        // Act
        List<Order> result = orderRepository.findLateOrders(TODAY);

        // Assert
        if (status.isOpen()) {
            assertThat(result)
                    .as("past-due order in open status %s must be late", status)
                    .extracting(Order::getStatus)
                    .containsExactly(status);
        } else {
            assertThat(result)
                    .as("past-due order in closed status %s must not be late", status)
                    .isEmpty();
        }
    }

    private Order orderPromisedOn(LocalDate promisedDeliveryDate) {
        return new Order(customer, promisedDeliveryDate.minusDays(7), promisedDeliveryDate, new BigDecimal("100.00"));
    }

    private Order orderForACustomerPromisedOn(Customer owner, LocalDate promisedDeliveryDate) {
        return new Order(owner, promisedDeliveryDate.minusDays(7), promisedDeliveryDate, new BigDecimal("100.00"));
    }

    private Order persist(Order order) {
        return entityManager.persist(order);
    }

    @Test
    void findLateOrdersLoadsCustomersInASingleQuery() {
        // Arrange:
        Customer qui = entityManager.persist(new Customer("Qui", "qui@test.example", null));
        Customer quo = entityManager.persist(new Customer("Quo", "quo@test.example", null));
        Customer qua = entityManager.persist(new Customer("Qua", "qua@test.example", null));

        persist(orderForACustomerPromisedOn(qui, TODAY.minusDays(2)));
        persist(orderForACustomerPromisedOn(quo, TODAY.minusDays(2)));
        persist(orderForACustomerPromisedOn(qua, TODAY.minusDays(2)));
        // persist does not write yet on the db, all the data are stored in a local cache.
        // flush() persist those data in the db.
        entityManager.flush();
        // we use clear() for wipe all the data written in the local cache, for made the application
        // read from the effective db and not from the cache.
        entityManager.clear();

        Statistics statistics = entityManager
                .getEntityManager()
                .getEntityManagerFactory()
                .unwrap(SessionFactory.class)
                .getStatistics();
        statistics.clear(); // azzera i contatori: contiamo solo quello che succede DOPO

        // Act:
        List<Order> lateOrders = orderRepository.findLateOrders(TODAY);

        // Assert:
        assertThat(lateOrders)
                .extracting(o -> o.getCustomer().getName())
                .containsExactlyInAnyOrder("Qui", "Quo", "Qua");

        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
    }
}
