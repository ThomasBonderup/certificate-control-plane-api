package com.combotto.controlplane.repositories;

import com.combotto.controlplane.model.BindingType;

public interface CertificateBindingTypeCount {
  BindingType getBindingType();

  long getCount();
}
