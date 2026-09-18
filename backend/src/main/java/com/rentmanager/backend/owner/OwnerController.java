package com.rentmanager.backend.owner;

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
@RequestMapping("/api/owners")
public class OwnerController {

  private final OwnerService ownerService;

  public OwnerController(OwnerService ownerService) {
    this.ownerService = ownerService;
  }

  @GetMapping
  public List<OwnerResponse> list() {
    return ownerService.list();
  }

  @GetMapping("/{id}")
  public OwnerResponse get(@PathVariable Long id) {
    return ownerService.get(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public OwnerResponse create(@Valid @RequestBody OwnerRequest request) {
    return ownerService.create(request);
  }

  @PutMapping("/{id}")
  public OwnerResponse update(@PathVariable Long id, @Valid @RequestBody OwnerRequest request) {
    return ownerService.update(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    ownerService.delete(id);
  }
}
