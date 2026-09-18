package com.rentmanager.backend.publicapi;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/properties")
public class PublicPropertyController {

  private final PublicPropertyService publicPropertyService;

  public PublicPropertyController(PublicPropertyService publicPropertyService) {
    this.publicPropertyService = publicPropertyService;
  }

  @GetMapping
  public List<PublicPropertyResponse> search(@RequestParam(name = "query", required = false) String query) {
    return publicPropertyService.search(query);
  }

  @GetMapping("/{id}")
  public PublicPropertyResponse get(@PathVariable Long id) {
    return publicPropertyService.get(id);
  }
}
