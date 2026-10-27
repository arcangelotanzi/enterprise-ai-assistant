package io.github.arcangelotanzi.assistant.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    @Test
    void findAllConvertsEveryCustomer() {
        when(customerRepository.findAll())
                .thenReturn(List.of(
                        new Customer("Rossi S.r.l.", "rossi@test.example", null),
                        new Customer("Bianchi S.p.A.", "bianchi@test.example", "+39 02 1234567")));

        List<CustomerResponse> result = customerService.findAll();

        assertThat(result).extracting(CustomerResponse::name).containsExactly("Rossi S.r.l.", "Bianchi S.p.A.");
    }

    @Test
    void findByIdReturnsTheCustomerWhenItExists() {
        when(customerRepository.findById(1L))
                .thenReturn(Optional.of(new Customer("Rossi S.r.l.", "rossi@test.example", null)));

        CustomerResponse result = customerService.findById(1L);

        assertThat(result.email()).isEqualTo("rossi@test.example");
    }

    @Test
    void findByIdThrowsWhenTheCustomerDoesNotExist() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.findById(99L))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void createSavesANewCustomerBuiltFromTheRequest() {
        when(customerRepository.findByEmail("rossi@test.example")).thenReturn(Optional.empty());
        // save() returns the entity it receives, like the real repository does
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        customerService.create(new CreateCustomerRequest("Rossi S.r.l.", "rossi@test.example", "+39 02 1234567"));

        // Capture the entity passed to save() and check every field landed in the right place
        ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(captor.capture());
        Customer saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("Rossi S.r.l.");
        assertThat(saved.getEmail()).isEqualTo("rossi@test.example");
        assertThat(saved.getPhone()).isEqualTo("+39 02 1234567");
    }

    @Test
    void createRejectsAnEmailAlreadyInUse() {
        when(customerRepository.findByEmail("rossi@test.example"))
                .thenReturn(Optional.of(new Customer("Rossi S.r.l.", "rossi@test.example", null)));

        assertThatThrownBy(() ->
                        customerService.create(new CreateCustomerRequest("Altro nome", "rossi@test.example", null)))
                .isInstanceOf(EmailAlreadyUsedException.class);

        verify(customerRepository, never()).save(any());
    }
}
