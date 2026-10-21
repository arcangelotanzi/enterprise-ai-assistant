package io.github.arcangelotanzi.assistant.customer;

import io.github.arcangelotanzi.assistant.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestcontainersConfiguration.class)
class CustomerRepositoryTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void saveAssignsGeneratedId() {
        Customer customer = new Customer("Arcangelo", "arci@test.example", "+39 3333333333");
        assertThat(customerRepository.save(customer).getId()).isNotNull();
    }

    @Test
    void createdAtIsFilledByTheDatabase() {
        Customer customer = new Customer("Arcangelo", "arci@test.example", "+39 3333333333");
        Customer saved = customerRepository.save(customer);
        entityManager.flush();
        entityManager.clear();
        Optional<Customer> retrieved = customerRepository.findById(saved.getId());
        assertThat(retrieved.orElseThrow().getCreatedAt()).isNotNull();
    }

    @Test
    void findByEmailReturnsTheCustomer() {
        Customer customer = new Customer("Arcangelo", "arci@test.example", "+39 3333333333");
        customerRepository.save(customer);
        Optional<Customer> byEmail = customerRepository.findByEmail("arci@test.example");
        assertThat(byEmail).isPresent();
        assertThat(byEmail.orElseThrow().getName()).isEqualTo("Arcangelo");
    }

    @Test
    void findByEmailReturnsEmptyWhenMissing() {
        assertThat(customerRepository.findByEmail("arci@tanzi.it")).isEmpty();
    }

    @Test
    void createdAtIsAvailableRightAfterSave() {
        Customer customer = new Customer("Arcangelo", "arci@test.example", "+39 3333333333");
        Customer saved = customerRepository.save(customer);
        assertThat(saved.getCreatedAt()).isNotNull();
    }
}