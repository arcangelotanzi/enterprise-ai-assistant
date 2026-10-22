package io.github.arcangelotanzi.assistant.order;

import io.github.arcangelotanzi.assistant.customer.Customer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final ZoneId ROME = ZoneId.of("Europe/Rome");

    @Mock
    private OrderRepository orderRepository;

    @Test
    void findLateOrdersComputesDaysLateAndIncludesContactDetails() {
        Customer customer = new Customer("Rossi S.r.l.", "rossi@test.example", "+39 02 1234567");
        Order order = new Order(customer, LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 18), new BigDecimal("1250.00"));
        when(orderRepository.findLateOrders(any())).thenReturn(List.of(order));

        List<LateOrderResponse> result = serviceAt("2026-10-21T10:00:00Z").findLateOrders();

        assertThat(result).singleElement().satisfies(late -> {
            assertThat(late.daysLate()).isEqualTo(3);
            assertThat(late.customerName()).isEqualTo("Rossi S.r.l.");
            assertThat(late.customerEmail()).isEqualTo("rossi@test.example");
            assertThat(late.customerPhone()).isEqualTo("+39 02 1234567");
            assertThat(late.status()).isEqualTo(OrderStatus.PENDING);
            assertThat(late.totalAmount()).isEqualByComparingTo("1250.00");
        });
    }

    @Test
    void findLateOrdersUsesTheBusinessTimeZoneToDecideToday() {
        // 23:30 UTC on Oct 20 is already 01:30 on Oct 21 in Rome
        when(orderRepository.findLateOrders(any())).thenReturn(List.of());

        serviceAt("2026-10-20T23:30:00Z").findLateOrders();

        verify(orderRepository).findLateOrders(LocalDate.of(2026, 10, 21));
    }

    @Test
    void findLateOrdersReturnsEmptyListWhenNothingIsLate() {
        when(orderRepository.findLateOrders(any())).thenReturn(List.of());

        assertThat(serviceAt("2026-10-21T10:00:00Z").findLateOrders()).isEmpty();
    }

    private OrderService serviceAt(String instant) {
        return new OrderService(orderRepository, Clock.fixed(Instant.parse(instant), ROME));
    }
}
