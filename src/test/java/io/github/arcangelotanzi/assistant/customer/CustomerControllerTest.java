package io.github.arcangelotanzi.assistant.customer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebMvcTest(CustomerController.class)
public class CustomerControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private CustomerService customerService;

    @Test
    void findByIdReturns404ProblemDetailWhenCustomerDoesNotExist() {
        when(customerService.findById(999L)).thenThrow(new CustomerNotFoundException(999L));

        assertThat(mvc.get().uri("/api/customers/{id}", 999))
                .hasStatus(HttpStatus.NOT_FOUND)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .extractingPath("$.detail").isEqualTo("No customer found with id 999");
    }

    @Test
    void findByIdReturnsTheCustomer() {
        when(customerService.findById(1L)).thenReturn(
                new CustomerResponse(1L,
                    "Rossi Forniture S.r.l.",
                    "acquisti@rossi.example",
                    "+39 02 1234567",
                    Instant.parse("2026-10-21T10:00:00Z")));

        assertThat(mvc.get().uri("/api/customers/{id}", 1))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("$.name").isEqualTo("Rossi Forniture S.r.l.");
    }

    @Test
    void createReturns201WithLocation() {
        when(customerService.create(
                new CreateCustomerRequest(
                        "arci",
                        "arci@tanzi.example",
                        null))).thenReturn(
                                new CustomerResponse(1L,
                                        "arci",
                                        "arci@tanzi.example",
                                        null,
                                        Instant.parse("2026-10-21T10:00:00Z")));

        assertThat(mvc.post().uri("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "name":"arci",
                            "email":"arci@tanzi.example"
                        }
                        """)
        ).hasStatus(HttpStatus.CREATED)
                .hasHeader("Location", "/api/customers/1")
                .bodyJson()
                .extractingPath("$.id").isEqualTo(1);
    }

    @Test
    void createReturns400WithFieldErrorsWhenNameIsBlank() {
        assertThat(mvc.post().uri("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"","email":"arci@tanzi.example"}
                        """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .extractingPath("$.errors.name").isEqualTo("must not be blank");

        // Validation must stop the request before it reaches the service
        verify(customerService, never()).create(any());
    }

    @Test
    void createReturns409WhenEmailIsAlreadyUsed() {
        when(customerService.create(any()))
                .thenThrow(new EmailAlreadyUsedException("arci@tanzi.example"));

        assertThat(mvc.post().uri("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"arci","email":"arci@tanzi.example"}
                        """))
                .hasStatus(HttpStatus.CONFLICT)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .extractingPath("$.detail")
                .isEqualTo("Email arci@tanzi.example is already used by another customer");
    }
}
