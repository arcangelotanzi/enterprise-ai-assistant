package io.github.arcangelotanzi.assistant.order;

import io.github.arcangelotanzi.assistant.customer.Customer;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.annotations.Generated;

@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // LAZY: loading the customer is decided per query (see OrderRepository#findLateOrders).
    // optional = false: an order cannot exist without a customer (customer_id is NOT NULL).
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private LocalDate orderDate;
    private LocalDate promisedDeliveryDate;
    private Instant deliveredAt;

    @Column(precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Generated
    @Column(insertable = false, updatable = false)
    private Instant createdAt;

    protected Order() {}

    public Order(Customer customer, LocalDate orderDate, LocalDate promisedDeliveryDate, BigDecimal totalAmount) {
        this.customer = customer;
        this.orderDate = orderDate;
        this.promisedDeliveryDate = promisedDeliveryDate;
        this.totalAmount = totalAmount;
        this.status = OrderStatus.PENDING;
    }

    public Long getId() {
        return id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public LocalDate getOrderDate() {
        return orderDate;
    }

    public LocalDate getPromisedDeliveryDate() {
        return promisedDeliveryDate;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isLateOn(LocalDate today) {
        return status.isOpen() && promisedDeliveryDate.isBefore(today);
    }

    public void markDelivered(Instant deliveredAt) {
        if (!this.status.isOpen()) {
            throw new IllegalStateException("Cannot mark order " + this.id + " as delivered: status is " + this.status);
        }
        this.status = OrderStatus.DELIVERED;
        this.deliveredAt = deliveredAt;
    }

    public void cancel() {
        if (!this.status.isOpen()) {
            throw new IllegalStateException("Cannot cancel order " + this.id + ": status is " + this.status);
        }
        this.status = OrderStatus.CANCELLED;
    }
}
