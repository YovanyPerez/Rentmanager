package com.rentmanager.backend.tenant;

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
@RequestMapping("/api/tenants")
public class TenantController {

  private final TenantService tenantService;

  public TenantController(TenantService tenantService) {
    this.tenantService = tenantService;
  }

  @GetMapping
  public List<TenantResponse> list() {
    return tenantService.list();
  }

  @GetMapping("/{id}")
  public TenantResponse get(@PathVariable Long id) {
    return tenantService.get(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public TenantResponse create(@Valid @RequestBody TenantRequest request) {
    return tenantService.create(request);
  }

  @PutMapping("/{id}")
  public TenantResponse update(@PathVariable Long id, @Valid @RequestBody TenantRequest request) {
    return tenantService.update(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    tenantService.delete(id);
  }
}
