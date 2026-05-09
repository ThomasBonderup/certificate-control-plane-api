package com.combotto.controlplane.services;

import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MultiGauge;
import io.micrometer.core.instrument.Tags;
import jakarta.annotation.PostConstruct;

@Component
public class CertificateOperationalMetrics {

  private static final Logger log = LoggerFactory.getLogger(CertificateOperationalMetrics.class);

  private final CertificatePostureService postureService;
  private final MultiGauge certificatesTotal;
  private final MultiGauge certificatesExpiringTotal;
  private final MultiGauge certificateBindingsTotal;
  private final AtomicLong expiredTotal = new AtomicLong();
  private final AtomicLong blockedRenewalsTotal = new AtomicLong();
  private final AtomicLong unboundTotal = new AtomicLong();
  private final AtomicLong nextExpiryTimestampSeconds = new AtomicLong();
  private final AtomicLong lastRefreshTimestampSeconds = new AtomicLong();
  private final AtomicLong refreshSuccess = new AtomicLong();

  public CertificateOperationalMetrics(
      CertificatePostureService postureService,
      MeterRegistry meterRegistry) {
    this.postureService = postureService;
    this.certificatesTotal = MultiGauge.builder("combotto.certificates.total")
        .description("Certificate records by lifecycle and renewal status.")
        .register(meterRegistry);
    this.certificatesExpiringTotal = MultiGauge.builder("combotto.certificates.expiring.total")
        .description("Certificate records expiring within the given window.")
        .register(meterRegistry);
    this.certificateBindingsTotal = MultiGauge.builder("combotto.certificate.bindings.total")
        .description("Certificate bindings by binding type.")
        .register(meterRegistry);

    Gauge.builder("combotto.certificates.expired.total", expiredTotal, AtomicLong::get)
        .description("Certificate records currently marked expired.")
        .register(meterRegistry);
    Gauge.builder("combotto.certificate.renewals.blocked.total", blockedRenewalsTotal, AtomicLong::get)
        .description("Certificate records whose renewal workflow is blocked.")
        .register(meterRegistry);
    Gauge.builder("combotto.certificates.unbound.total", unboundTotal, AtomicLong::get)
        .description("Certificate records without any asset binding.")
        .register(meterRegistry);
    Gauge.builder("combotto.certificate.next.expiry.timestamp.seconds", nextExpiryTimestampSeconds, AtomicLong::get)
        .description("Unix timestamp of the next known certificate expiry.")
        .register(meterRegistry);
    Gauge.builder("combotto.certificate.metrics.last.refresh.timestamp.seconds", lastRefreshTimestampSeconds, AtomicLong::get)
        .description("Unix timestamp of the last successful certificate metrics refresh.")
        .register(meterRegistry);
    Gauge.builder("combotto.certificate.metrics.refresh.success", refreshSuccess, AtomicLong::get)
        .description("Whether the latest certificate metrics refresh succeeded.")
        .register(meterRegistry);
  }

  @PostConstruct
  public void refreshOnStartup() {
    refresh();
  }

  @Scheduled(fixedDelayString = "${control-plane.metrics.certificates.refresh-delay-ms:30000}")
  public void refresh() {
    try {
      CertificatePostureSnapshot snapshot = postureService.snapshot();
      publish(snapshot);
      refreshSuccess.set(1);
      lastRefreshTimestampSeconds.set(System.currentTimeMillis() / 1000);
    } catch (RuntimeException ex) {
      refreshSuccess.set(0);
      log.warn("Failed to refresh certificate operational metrics", ex);
    }
  }

  private void publish(CertificatePostureSnapshot snapshot) {
    certificatesTotal.register(snapshot.certificateCounts().entrySet().stream()
        .map(entry -> MultiGauge.Row.of(
            Tags.of(
                "status", entry.getKey().status(),
                "renewal_status", entry.getKey().renewalStatus()),
            entry.getValue()))
        .toList(), true);

    certificatesExpiringTotal.register(snapshot.expiringCountsByWindowDays().entrySet().stream()
        .map(entry -> MultiGauge.Row.of(
            Tags.of("window", entry.getKey() + "d"),
            entry.getValue()))
        .toList(), true);

    certificateBindingsTotal.register(snapshot.bindingCountsByType().entrySet().stream()
        .map(entry -> MultiGauge.Row.of(
            Tags.of("binding_type", entry.getKey()),
            entry.getValue()))
        .toList(), true);

    expiredTotal.set(snapshot.expiredCount());
    blockedRenewalsTotal.set(snapshot.blockedRenewalCount());
    unboundTotal.set(snapshot.unboundCount());
    nextExpiryTimestampSeconds.set(toEpochSeconds(snapshot.nextExpiry()));
  }

  private long toEpochSeconds(OffsetDateTime timestamp) {
    return timestamp == null ? 0 : timestamp.toEpochSecond();
  }
}
