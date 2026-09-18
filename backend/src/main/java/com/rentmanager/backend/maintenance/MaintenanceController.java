package com.rentmanager.backend.maintenance;

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
@RequestMapping("/api/maintenance")
public class MaintenanceController {

  private final MaintenanceService maintenanceService;

  public MaintenanceController(MaintenanceService maintenanceService) {
    this.maintenanceService = maintenanceService;
  }

  @GetMapping
  public List<MaintenanceResponse> list() {
    return maintenanceService.list();
  }

  @GetMapping("/{id}")
  public MaintenanceResponse get(@PathVariable Long id) {
    return maintenanceService.get(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public MaintenanceResponse create(@Valid @RequestBody MaintenanceCreateRequest request) {
    return maintenanceService.create(request);
  }

  @PutMapping("/{id}")
  public MaintenanceResponse update(@PathVariable Long id, @Valid @RequestBody MaintenanceUpdateRequest request) {
    return maintenanceService.update(id, request);
  }
}
