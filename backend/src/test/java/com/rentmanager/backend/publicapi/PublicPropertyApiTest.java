package com.rentmanager.backend.publicapi;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rentmanager.backend.domain.Owner;
import com.rentmanager.backend.domain.Property;
import com.rentmanager.backend.domain.PropertyStatus;
import com.rentmanager.backend.repository.OwnerRepository;
import com.rentmanager.backend.repository.PropertyRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PublicPropertyApiTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private OwnerRepository owners;

  @Autowired
  private PropertyRepository properties;

  @Test
  void anonymousSeesOnlyAvailablePropertiesWithoutOwnerData() throws Exception {
    Property available = newProperty("Calle Pública 1", "CiudadPublica", PropertyStatus.AVAILABLE);
    Property maintenance = newProperty("Calle Privada 2", "CiudadPublica", PropertyStatus.MAINTENANCE);

    String body = mockMvc.perform(get("/api/public/properties"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].ownerId").doesNotExist())
        .andExpect(jsonPath("$[0].ownerName").doesNotExist())
        .andReturn().getResponse().getContentAsString();

    expectContains(body, available.getId(), true);
    expectContains(body, maintenance.getId(), false);
  }

  @Test
  void searchFiltersByCityOrAddress() throws Exception {
    Property bilbao = newProperty("Gran Vía 1", "BilbaoPublico", PropertyStatus.AVAILABLE);
    newProperty("Calle Otra 2", "MadridPublico", PropertyStatus.AVAILABLE);

    String body = mockMvc.perform(get("/api/public/properties").param("query", "bilbaopublico"))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();

    expectContains(body, bilbao.getId(), true);
  }

  @Test
  void availableDetailReturnsPublicData() throws Exception {
    Property property = newProperty("Calle Detalle 3", "DetalleCiudad", PropertyStatus.AVAILABLE);
    property.setImageUrl("/assets/properties/demo.jpg");
    properties.save(property);

    mockMvc.perform(get("/api/public/properties/{id}", property.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.address").value("Calle Detalle 3"))
        .andExpect(jsonPath("$.monthlyRent").value(1000.00))
        .andExpect(jsonPath("$.imageUrl").value("/assets/properties/demo.jpg"))
        .andExpect(jsonPath("$.status").value("AVAILABLE"))
        .andExpect(jsonPath("$.ownerId").doesNotExist());
  }

  @Test
  void unavailableDetailReturnsNotFound() throws Exception {
    Property property = newProperty("Calle Ocupada 4", "DetalleCiudad", PropertyStatus.RENTED);

    mockMvc.perform(get("/api/public/properties/{id}", property.getId()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("PROPERTY_NOT_FOUND"));
  }

  private void expectContains(String json, Long propertyId, boolean expected) {
    boolean contains = json.contains("\"id\":" + propertyId + ",") || json.contains("\"id\":" + propertyId + "}");
    if (contains != expected) {
      throw new AssertionError(
          "Expected property " + propertyId + (expected ? " in " : " not in ") + "response: " + json);
    }
  }

  private Property newProperty(String address, String city, PropertyStatus status) {
    Owner owner = new Owner();
    owner.setFullName("Public Owner " + System.nanoTime());
    owners.save(owner);
    Property property = new Property();
    property.setOwner(owner);
    property.setAddress(address);
    property.setCity(city);
    property.setMonthlyRent(new BigDecimal("1000.00"));
    property.setStatus(status);
    return properties.save(property);
  }
}
