package com.combotto.controlplane;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.combotto.controlplane.services.CertificateOperationalMetrics;
import com.combotto.controlplane.services.CertificatePostureService;
import com.combotto.controlplane.services.CertificatePostureSnapshot;
import com.combotto.controlplane.services.CertificateStatusRenewalKey;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class CertificateOperationalMetricsTest {

  @Test
  void refreshPublishesExpectedLowCardinalityMetrics() {
    CertificatePostureService postureService = org.mockito.Mockito.mock(CertificatePostureService.class);
    when(postureService.snapshot()).thenReturn(new CertificatePostureSnapshot(
        Map.of(new CertificateStatusRenewalKey("ACTIVE", "NOT_STATUS"), 7L),
        Map.of(30, 0L, 60, 0L, 90, 0L),
        0L,
        0L,
        1L,
        Map.of("MQTT_DEVICE_CLIENT_AUTH", 3L, "TRUST_BUNDLE", 2L),
        OffsetDateTime.parse("2027-12-16T13:55:40Z")));

    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    CertificateOperationalMetrics metrics = new CertificateOperationalMetrics(postureService, registry);

    metrics.refresh();

    assertEquals(7.0, registry.get("combotto.certificates.total")
        .tag("status", "ACTIVE")
        .tag("renewal_status", "NOT_STATUS")
        .gauge()
        .value());
    assertEquals(0.0, registry.get("combotto.certificates.expiring.total")
        .tag("window", "90d")
        .gauge()
        .value());
    assertEquals(3.0, registry.get("combotto.certificate.bindings.total")
        .tag("binding_type", "MQTT_DEVICE_CLIENT_AUTH")
        .gauge()
        .value());
    assertEquals(1.0, registry.get("combotto.certificates.unbound.total").gauge().value());
    assertEquals(1828965340.0, registry.get("combotto.certificate.next.expiry.timestamp.seconds").gauge().value());
    assertEquals(1.0, registry.get("combotto.certificate.metrics.refresh.success").gauge().value());

    Set<String> tagKeys = registry.getMeters().stream()
        .flatMap(meter -> meter.getId().getTags().stream())
        .map(tag -> tag.getKey())
        .collect(Collectors.toSet());

    assertFalse(tagKeys.contains("certificate_id"));
    assertFalse(tagKeys.contains("name"));
    assertFalse(tagKeys.contains("common_name"));
    assertFalse(tagKeys.contains("sha256_fingerprint"));
    assertFalse(tagKeys.contains("endpoint"));
    assertFalse(tagKeys.contains("notes"));
  }

  @Test
  void refreshFailureKeepsLastValuesAndMarksRefreshUnsuccessful() {
    CertificatePostureService postureService = org.mockito.Mockito.mock(CertificatePostureService.class);
    when(postureService.snapshot())
        .thenReturn(new CertificatePostureSnapshot(
            Map.of(new CertificateStatusRenewalKey("ACTIVE", "NOT_STATUS"), 7L),
            Map.of(30, 0L, 60, 0L, 90, 0L),
            0L,
            0L,
            1L,
            Map.of(),
            OffsetDateTime.parse("2027-12-16T13:55:40Z")))
        .thenThrow(new IllegalStateException("database unavailable"));

    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    CertificateOperationalMetrics metrics = new CertificateOperationalMetrics(postureService, registry);

    metrics.refresh();
    metrics.refresh();

    assertEquals(7.0, registry.get("combotto.certificates.total")
        .tag("status", "ACTIVE")
        .tag("renewal_status", "NOT_STATUS")
        .gauge()
        .value());
    assertEquals(0.0, registry.get("combotto.certificate.metrics.refresh.success").gauge().value());
  }
}
