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
 * Editorial state of a {@link Part} — the first of the three orthogonal status
 * dimensions. Fixed order with free transitions; several terminal states stand
 * side by side.
 */
public enum EditorialState {
  BACKLOG,
  IN_PLANNING,
  IN_PROGRESS,
  REVIEW,
  DONE,
  SKIPPED,
  CANCELLED;

  /** The terminal states of this dimension. */
  public boolean isTerminal() {
    return this == DONE || this == SKIPPED || this == CANCELLED;
  }
}
