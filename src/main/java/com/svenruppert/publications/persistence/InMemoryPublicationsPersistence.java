/*
 * Copyright © 2013 Sven Ruppert (sven.ruppert@gmail.com)
 *
 * Licensed under the EUPL, Version 1.2 (the "Licence");
 * you may not use this file except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *     https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */

package com.svenruppert.publications.persistence;

import com.svenruppert.publications.model.DataRoot;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.Objects;

/**
 * Heap-based {@link PublicationsPersistence} — the no-mocks test seam. Holds the
 * data root as a live reference; {@link #save} merely remembers the (in-place
 * mutated) root, {@link #load} returns it.
 */
public final class InMemoryPublicationsPersistence implements PublicationsPersistence {

  private DataRoot root;

  public InMemoryPublicationsPersistence() {
    this(new DataRoot());
  }

  public InMemoryPublicationsPersistence(DataRoot initial) {
    this.root = Objects.requireNonNull(initial, "initial");
  }

  @Override
  @SuppressFBWarnings(value = "EI_EXPOSE_REP",
      justification = "by design — the test seam returns the live root that callers mutate in "
          + "place, mirroring the EclipseStore persistence semantics")
  public synchronized DataRoot load() {
    return root;
  }

  @Override
  public synchronized void save(DataRoot root) {
    this.root = Objects.requireNonNull(root, "root");
  }
}
