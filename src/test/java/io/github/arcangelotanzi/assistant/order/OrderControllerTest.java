package io.github.arcangelotanzi.assistant.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private OrderService orderService;

    @Test
    void lateReturnsLateOrdersAndIsNotMistakenForAnId() {
        when(orderService.findLateOrders())
                .thenReturn(List.of(new LateOrderResponse(
                        8L,
                        4L,
                        "Giovannino S.p.a.",
                        "contatti@giovanninospa.example",
                        null,
                        OrderStatus.SHIPPED,
                        LocalDate.of(2026, 10, 11),
                        11,
                        new BigDecimal("2100.00"))));

        assertThat(mvc.get().uri("/api/orders/late")).hasStatusOk().bodyJson().isLenientlyEqualTo("""
                        [{"orderId":8,"customerEmail":"contatti@giovanninospa.example","daysLate":11}]
                        """);

        // "/late" must hit the literal mapping, never the "/{id}" one
        verify(orderService, never()).findById(any());
    }

    @Test
    void findByIdReturns404WhenOrderDoesNotExist() {
        when(orderService.findById(99L)).thenThrow(new OrderNotFoundException(99L));

        assertThat(mvc.get().uri("/api/orders/{id}", 99))
                .hasStatus(HttpStatus.NOT_FOUND)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .extractingPath("$.detail")
                .isEqualTo("No order found with id 99");
    }

    @Test
    void createReturns201WithLocation() {
        when(orderService.create(any()))
                .thenReturn(new OrderResponse(
                        11L,
                        1L,
                        "Rossi Forniture S.r.l.",
                        OrderStatus.PENDING,
                        LocalDate.of(2026, 10, 22),
                        LocalDate.of(2026, 11, 5),
                        null,
                        new BigDecimal("99.90"),
                        Instant.parse("2026-10-22T14:45:29Z")));

        assertThat(mvc.post()
                        .uri("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {"customerId":1,"promisedDeliveryDate":"2026-11-05","totalAmount":99.90}
                        """))
                .hasStatus(HttpStatus.CREATED)
                .hasHeader("Location", "/api/orders/11")
                .bodyJson()
                // LocalDate must be serialized as an ISO string, not as [2026,11,5]
                .extractingPath("$.promisedDeliveryDate")
                .isEqualTo("2026-11-05");
    }

    @Test
    void createReturns422WhenDeliveryDateIsInThePast() {
        when(orderService.create(any()))
                .thenThrow(new InvalidDeliveryDateException(LocalDate.of(2020, 1, 1), LocalDate.of(2026, 10, 22)));

        assertThat(mvc.post()
                        .uri("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {"customerId":1,"promisedDeliveryDate":"2020-01-01","totalAmount":10.00}
                        """))
                .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .extractingPath("$.detail")
                .asString()
                .contains("2020-01-01");
    }

    @Test
    void createReturns400WhenTotalAmountIsMissing() {
        assertThat(mvc.post()
                        .uri("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {"customerId":1,"promisedDeliveryDate":"2026-11-05"}
                        """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.errors.totalAmount")
                .isEqualTo("must not be null");

        verify(orderService, never()).create(any());
    }

    @Test
    void createReturns400WhenTotalAmountHasTooManyDecimals() {
        // Valid JSON and a positive number: only @Digits(fraction = 2) can reject it
        assertThat(mvc.post()
                        .uri("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {"customerId":1,"promisedDeliveryDate":"2026-11-05","totalAmount":10.999}
                        """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.errors.totalAmount")
                .asString()
                .contains("numeric value out of bounds");

        verify(orderService, never()).create(any());
    }
}
