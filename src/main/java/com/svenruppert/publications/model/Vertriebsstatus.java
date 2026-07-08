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

package com.svenruppert.publications.model;

/**
 * Akquise- (reduzierter Vertriebs-) Status einer {@link Veroeffentlichung} —
 * die dritte orthogonale Statusdimension, ohne kaufmännische Abrechnung.
 * Feste Reihenfolge bei freien Übergängen; mehrere Endzustände.
 */
public enum Vertriebsstatus {
  REQUESTED,
  OFFERED,
  ACCEPTED,
  REVIEW,
  DONE,
  SKIPPED,
  CANCELLED;

  /** Die Endzustände dieser Dimension. */
  public boolean isEndzustand() {
    return this == DONE || this == SKIPPED || this == CANCELLED;
  }
}
