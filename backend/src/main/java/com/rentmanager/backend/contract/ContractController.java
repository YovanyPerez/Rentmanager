package com.rentmanager.backend.contract;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contracts")
public class ContractController {

  private final ContractService contractService;

  public ContractController(ContractService contractService) {
    this.contractService = contractService;
  }

  @GetMapping
  public List<ContractResponse> list() {
    return contractService.list();
  }

  @GetMapping("/{id}")
  public ContractResponse get(@PathVariable Long id) {
    return contractService.get(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ContractResponse create(@Valid @RequestBody ContractRequest request) {
    return contractService.create(request);
  }

  @PutMapping("/{id}")
  public ContractResponse update(@PathVariable Long id, @Valid @RequestBody ContractRequest request) {
    return contractService.update(id, request);
  }

  @PostMapping("/{id}/activate")
  public ContractResponse activate(@PathVariable Long id) {
    return contractService.activate(id);
  }

  @PostMapping("/{id}/terminate")
  public ContractResponse terminate(@PathVariable Long id) {
    return contractService.terminate(id);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    contractService.delete(id);
  }
}
