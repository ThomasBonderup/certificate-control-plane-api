package com.combotto.controlplane.common;

import java.util.List;
import java.util.regex.Pattern;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PageableSanitizer {

  private static final Pattern SAFE_SORT_PROPERTY = Pattern.compile("^[A-Za-z][A-Za-z0-9_.]*$");

  private PageableSanitizer() {
  }

  public static Pageable sanitize(Pageable pageable, Sort fallbackSort) {
    List<Sort.Order> validOrders = pageable.getSort().stream()
        .filter(order -> SAFE_SORT_PROPERTY.matcher(order.getProperty()).matches())
        .toList();

    Sort sort = validOrders.isEmpty() ? fallbackSort : Sort.by(validOrders);

    return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
  }
}
