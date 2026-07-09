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

/**
 * Storage abstraction for the {@link DataRoot} object graph of the publications
 * domain. Mirrors the {@code UserDirectoryPersistence} pattern: an in-memory
 * variant as a test seam and an Eclipse-Store variant for production.
 */
public interface PublicationsPersistence {

  /** Loads the data root; returns an empty root on first start. */
  DataRoot load();

  /** Persists the state of the (in-place mutated) data root. */
  void save(DataRoot root);

  /** Releases resources (idempotent). In-memory variants do nothing. */
  default void close() {
  }
}
