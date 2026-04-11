error id: file://<WORKSPACE>/src/main/java/com/combotto/controlplane/api/AuditRunMapper.java:_empty_/AuditRunResponse#
file://<WORKSPACE>/src/main/java/com/combotto/controlplane/api/AuditRunMapper.java
empty definition using pc, found symbol in pc: _empty_/AuditRunResponse#
empty definition using semanticdb
empty definition using fallback
non-local guesses:

offset: 175
uri: file://<WORKSPACE>/src/main/java/com/combotto/controlplane/api/AuditRunMapper.java
text:
```scala
package com.combotto.controlplane.api;

import com.combotto.controlplane.model.AuditRun;

public class AuditRunMapper {
  private AuditRunMapper() {
  }

  public static Audit@@RunResponse toResponse(AuditRun run) {
    return new AuditRunResponse(
        run.id(),
        run.assetId(),
        run.profile(),
        run.status());
  }

}

```


#### Short summary: 

empty definition using pc, found symbol in pc: _empty_/AuditRunResponse#