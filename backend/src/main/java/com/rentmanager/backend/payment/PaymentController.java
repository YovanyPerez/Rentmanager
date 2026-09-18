package com.rentmanager.backend.payment;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

  private final PaymentService paymentService;

  public PaymentController(PaymentService paymentService) {
    this.paymentService = paymentService;
  }

  @GetMapping
  public List<PaymentResponse> list() {
    return paymentService.list();
  }

  @GetMapping("/{id}")
  public PaymentResponse get(@PathVariable Long id) {
    return paymentService.get(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public PaymentResponse create(@Valid @RequestBody PaymentRequest request) {
    return paymentService.create(request);
  }

  @PutMapping("/{id}")
  public PaymentResponse update(@PathVariable Long id, @Valid @RequestBody PaymentUpdateRequest request) {
    return paymentService.update(id, request);
  }
}
