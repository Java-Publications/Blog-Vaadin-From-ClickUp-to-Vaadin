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

import java.time.Instant;
import java.util.Objects;

/**
 * An immutable status-change event within a {@link StatusHistory}.
 *
 * <p>The {@code sequence} starts at {@code 0} per history and grows by one with
 * every event; it — not the list position and not the {@code timestamp} — is the
 * authoritative ordering criterion. The {@code timestamp} stays descriptive,
 * since clocks can jump.
 *
 * @param sequence  strictly ascending sequence number, from {@code 0}
 * @param from      source state (the {@code current()} value before the change)
 * @param to        target state
 * @param actor     name of the acting actor (may be {@code null})
 * @param timestamp descriptive timestamp of the change
 */
public record StatusChange<S extends Enum<S>>(
    long sequence, S from, S to, String actor, Instant timestamp) {

  public StatusChange {
    if (sequence < 0) {
      throw new IllegalArgumentException("sequence must be >= 0, was " + sequence);
    }
    Objects.requireNonNull(from, "from");
    Objects.requireNonNull(to, "to");
    Objects.requireNonNull(timestamp, "timestamp");
  }
}
