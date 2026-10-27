package io.github.arcangelotanzi.assistant.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.arcangelotanzi.assistant.customer.Customer;
import io.github.arcangelotanzi.assistant.customer.CustomerNotFoundException;
import io.github.arcangelotanzi.assistant.customer.CustomerRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final ZoneId ROME = ZoneId.of("Europe/Rome");

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Test
    void findLateOrdersComputesDaysLateAndIncludesContactDetails() {
        Customer customer = new Customer("Rossi S.r.l.", "rossi@test.example", "+39 02 1234567");
        Order order =
                new Order(customer, LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 18), new BigDecimal("1250.00"));
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

    @Test
    void createRejectsAPromisedDateInThePastWithoutTouchingTheDatabase() {
        // "Now" is Oct 21 in Rome, the request promises Oct 20
        CreateOrderRequest request = new CreateOrderRequest(1L, LocalDate.of(2026, 10, 20), new BigDecimal("100.00"));

        assertThatThrownBy(() -> serviceAt("2026-10-21T10:00:00Z").create(request))
                .isInstanceOf(InvalidDeliveryDateException.class)
                .hasMessageContaining("2026-10-20")
                .hasMessageContaining("2026-10-21");

        verify(customerRepository, never()).findById(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createAcceptsAPromisedDateOfToday() {
        Customer customer = new Customer("Rossi S.r.l.", "rossi@test.example", null);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse result = serviceAt("2026-10-21T10:00:00Z")
                .create(new CreateOrderRequest(1L, LocalDate.of(2026, 10, 21), new BigDecimal("100.00")));

        assertThat(result.promisedDeliveryDate()).isEqualTo(LocalDate.of(2026, 10, 21));
    }

    @Test
    void createRejectsAnUnknownCustomer() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());
        CreateOrderRequest request = new CreateOrderRequest(99L, LocalDate.of(2026, 11, 5), new BigDecimal("100.00"));

        assertThatThrownBy(() -> serviceAt("2026-10-21T10:00:00Z").create(request))
                .isInstanceOf(CustomerNotFoundException.class);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void createSavesAPendingOrderDatedTodayByTheClock() {
        Customer customer = new Customer("Rossi S.r.l.", "rossi@test.example", null);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // 23:30 UTC on Oct 20 is Oct 21 in Rome: the order must be dated Oct 21
        serviceAt("2026-10-20T23:30:00Z")
                .create(new CreateOrderRequest(1L, LocalDate.of(2026, 11, 5), new BigDecimal("1250.00")));

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        Order saved = captor.getValue();
        assertThat(saved.getOrderDate()).isEqualTo(LocalDate.of(2026, 10, 21));
        assertThat(saved.getPromisedDeliveryDate()).isEqualTo(LocalDate.of(2026, 11, 5));
        assertThat(saved.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(saved.getCustomer()).isSameAs(customer);
        assertThat(saved.getTotalAmount()).isEqualByComparingTo("1250.00");
    }

    @Test
    void findByIdThrowsWhenTheOrderDoesNotExist() {
        when(orderRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> serviceAt("2026-10-21T10:00:00Z").findById(42L))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining("42");
    }

    private OrderService serviceAt(String instant) {
        return new OrderService(orderRepository, customerRepository, Clock.fixed(Instant.parse(instant), ROME));
    }
}
