package com.rentmanager.backend.property;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/properties")
public class PropertyController {

  private final PropertyService propertyService;

  public PropertyController(PropertyService propertyService) {
    this.propertyService = propertyService;
  }

  @GetMapping
  public List<PropertyResponse> list() {
    return propertyService.list();
  }

  @GetMapping("/{id}")
  public PropertyResponse get(@PathVariable Long id) {
    return propertyService.get(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public PropertyResponse create(@Valid @RequestBody PropertyRequest request) {
    return propertyService.create(request);
  }

  @PutMapping("/{id}")
  public PropertyResponse update(@PathVariable Long id, @Valid @RequestBody PropertyRequest request) {
    return propertyService.update(id, request);
  }

  @PatchMapping("/{id}/status")
  public PropertyResponse changeStatus(@PathVariable Long id,
      @Valid @RequestBody PropertyStatusRequest request) {
    return propertyService.changeStatus(id, request.status());
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deactivate(@PathVariable Long id) {
    propertyService.deactivate(id);
  }
}
