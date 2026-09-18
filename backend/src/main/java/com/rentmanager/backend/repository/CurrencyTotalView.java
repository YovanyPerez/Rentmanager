package com.rentmanager.backend.repository;

import com.rentmanager.backend.domain.CurrencyCode;
import java.math.BigDecimal;

/** Projection for paid amounts grouped by currency. */
public interface CurrencyTotalView {

  CurrencyCode getCurrency();

  BigDecimal getTotal();
}
