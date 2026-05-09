package com.combotto.controlplane.repositories;

import com.combotto.controlplane.model.CertificateStatus;
import com.combotto.controlplane.model.RenewalStatus;

public interface CertificateStatusRenewalCount {
  CertificateStatus getStatus();

  RenewalStatus getRenewalStatus();

  long getCount();
}
