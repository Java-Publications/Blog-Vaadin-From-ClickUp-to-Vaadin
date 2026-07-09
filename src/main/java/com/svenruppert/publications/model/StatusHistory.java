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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A hardened, append-only status history of a single status dimension. It fully
 * encapsulates the event list: to the outside it offers only an immutable view
 * ({@link #events()}) and, as the sole mutating capability, appending
 * ({@link #record}). There are no methods to insert, remove or reorder.
 *
 * <p>Because {@code record} derives the source value {@code from} itself from the
 * current state and assigns the next sequence number, neither an inconsistent
 * transition can be injected nor the order falsified. The {@link #current()
 * current state} is always the target of the last event and therefore cannot
 * diverge from the history.
 *
 * <p>The type deliberately carries no {@link java.time.Clock} field, so the
 * persisted object graph stays free of runtime infrastructure; the timestamp is
 * created on append or (import, test) passed in explicitly.
 *
 * @param <S> the enum status dimension
 */
public final class StatusHistory<S extends Enum<S>> {

  private final S initial;
  private final List<StatusChange<S>> events = new ArrayList<>();

  /**
   * @param initial source state before the first change; never {@code null}
   */
  public StatusHistory(S initial) {
    this.initial = Objects.requireNonNull(initial, "initial");
  }

  /** Source state the history was initialized with. */
  public S initial() {
    return initial;
  }

  /**
   * @return the target of the last event, otherwise the initial state
   */
  public S current() {
    return events.isEmpty()
        ? initial
        : events.get(events.size() - 1).to();
  }

  /** {@code true} while no change has been recorded yet. */
  public boolean isEmpty() {
    return events.isEmpty();
  }

  /** Number of changes recorded so far (also the next sequence number). */
  public int count() {
    return events.size();
  }

  /**
   * Appends a status change. {@code from} is derived from {@link #current()}, the
   * {@code sequence} is the number of prior events, the timestamp is
   * {@link Instant#now()}.
   *
   * @param to    the new state; never {@code null}
   * @param actor name of the actor (may be {@code null})
   */
  public void record(S to, String actor) {
    record(to, actor, Instant.now());
  }

  /**
   * Like {@link #record(Enum, String)}, but with an explicit timestamp — for the
   * idempotent import (carrying over the original timestamp) and for
   * deterministic tests.
   */
  public void record(S to, String actor, Instant timestamp) {
    Objects.requireNonNull(to, "to");
    Objects.requireNonNull(timestamp, "timestamp");
    S from = current();
    events.add(new StatusChange<>(events.size(), from, to, actor, timestamp));
  }

  /** Immutable view of the event chain, ordered by sequence number. */
  public List<StatusChange<S>> events() {
    return List.copyOf(events);
  }
}
