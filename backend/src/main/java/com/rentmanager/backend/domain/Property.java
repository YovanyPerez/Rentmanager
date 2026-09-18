package com.rentmanager.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "properties")
public class Property {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "owner_id", nullable = false)
  private Owner owner;

  @Column(nullable = false)
  private String address;

  @Column(nullable = false, length = 100)
  private String city;

  @Column(length = 2000)
  private String description;

  @Column(name = "monthly_rent", nullable = false, precision = 10, scale = 2)
  private BigDecimal monthlyRent;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private PropertyStatus status = PropertyStatus.AVAILABLE;

  @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
  private Instant updatedAt;

  public Long getId() {
    return id;
  }

  public Owner getOwner() {
    return owner;
  }

  public void setOwner(Owner owner) {
    this.owner = owner;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public String getCity() {
    return city;
  }

  public void setCity(String city) {
    this.city = city;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public BigDecimal getMonthlyRent() {
    return monthlyRent;
  }

  public void setMonthlyRent(BigDecimal monthlyRent) {
    this.monthlyRent = monthlyRent;
  }

  public PropertyStatus getStatus() {
    return status;
  }

  public void setStatus(PropertyStatus status) {
    this.status = status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
