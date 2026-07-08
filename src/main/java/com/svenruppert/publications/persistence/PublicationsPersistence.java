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

import com.svenruppert.publications.model.Datenwurzel;

/**
 * Speicher-Abstraktion für den {@link Datenwurzel}-Objektgraph der
 * Publikationsverwaltung. Spiegelt das Muster von
 * {@code UserDirectoryPersistence}: eine In-Memory-Variante als Test-Seam und
 * eine Eclipse-Store-Variante für die Produktion.
 */
public interface PublicationsPersistence {

  /** Lädt die Datenwurzel; liefert beim ersten Start eine leere Wurzel. */
  Datenwurzel load();

  /** Persistiert den Zustand der (in place mutierten) Datenwurzel. */
  void save(Datenwurzel wurzel);

  /** Gibt Ressourcen frei (idempotent). In-Memory-Varianten machen nichts. */
  default void close() {
  }
}
