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

import java.util.Objects;

/**
 * Heap-basierte {@link PublicationsPersistence} — der No-Mocks-Test-Seam.
 * Hält die Datenwurzel als lebende Referenz; {@link #save} merkt sich lediglich
 * die (in place mutierte) Wurzel, {@link #load} gibt sie zurück.
 */
public final class InMemoryPublicationsPersistence implements PublicationsPersistence {

  private Datenwurzel wurzel;

  public InMemoryPublicationsPersistence() {
    this(new Datenwurzel());
  }

  public InMemoryPublicationsPersistence(Datenwurzel initial) {
    this.wurzel = Objects.requireNonNull(initial, "initial");
  }

  @Override
  public synchronized Datenwurzel load() {
    return wurzel;
  }

  @Override
  public synchronized void save(Datenwurzel wurzel) {
    this.wurzel = Objects.requireNonNull(wurzel, "wurzel");
  }
}
