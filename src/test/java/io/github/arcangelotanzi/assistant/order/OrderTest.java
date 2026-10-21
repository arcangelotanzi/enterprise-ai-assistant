package io.github.arcangelotanzi.assistant.order;

import io.github.arcangelotanzi.assistant.customer.Customer;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

public class OrderTest {

    @Test
    void openOrderDeliveryPromisedYesterday() {
        Customer c = new Customer("Arci", "arci@prova.example", null);
        LocalDate today = LocalDate.of(2026, 10, 21);
        Order order = new Order(c, today.minusDays(20), today.minusDays(1), BigDecimal.ONE);
        assertThat(order.isLateOn(today)).isTrue();
    }

    @Test
    void openOrderDeliveryPromisedToday() {
        Customer c = new Customer("Arci", "arci@prova.example", null);
        LocalDate today = LocalDate.of(2026, 10, 21);
        Order order = new Order(c, today.minusDays(20), today, BigDecimal.ONE);
        assertThat(order.isLateOn(today)).isFalse();
    }

    @Test
    void openOrderDeliveryPromisedTomorrow() {
        Customer c = new Customer("Arci", "arci@prova.example", null);
        LocalDate today = LocalDate.of(2026, 10, 21);
        Order order = new Order(c, today.minusDays(20), today.plusDays(1), BigDecimal.ONE);
        assertThat(order.isLateOn(today)).isFalse();
    }

    @Test
    void deliveredOrderDeliveryPromisedYesterday() {
        Customer c = new Customer("Arci", "arci@prova.example", null);
        LocalDate today = LocalDate.of(2026, 10, 21);
        Order order = new Order(c, today.minusDays(20), today.minusDays(1), BigDecimal.ONE);
        order.markDelivered(Instant.now());
        assertThat(order.isLateOn(today)).isFalse();
    }

    @Test
    void cancelledOrderDeliveryPromisedYesterday() {
        Customer c = new Customer("Arci", "arci@prova.example", null);
        LocalDate today = LocalDate.of(2026, 10, 21);
        Order order = new Order(c, today.minusDays(20), today.minusDays(1), BigDecimal.ONE);
        order.cancel();
        assertThat(order.isLateOn(today)).isFalse();
    }

    @Test
    void markDeliveredOnCancelledOrder() {
        Customer c = new Customer("Arci", "arci@prova.example", null);
        LocalDate today = LocalDate.of(2026, 10, 21);
        Order order = new Order(c, today.minusDays(20), today.minusDays(1), BigDecimal.ONE);
        order.cancel();
        assertThatThrownBy(() -> order.markDelivered(Instant.now()))
                .isInstanceOf(IllegalStateException.class);
    }
}
