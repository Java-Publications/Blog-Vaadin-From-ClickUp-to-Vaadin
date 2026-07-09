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

package junit.com.svenruppert.publications.model;

import com.svenruppert.publications.model.AcquisitionStatus;
import com.svenruppert.publications.model.EditorialState;
import com.svenruppert.publications.model.StatusChange;
import com.svenruppert.publications.model.StatusHistory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static com.svenruppert.publications.model.EditorialState.BACKLOG;
import static com.svenruppert.publications.model.EditorialState.DONE;
import static com.svenruppert.publications.model.EditorialState.IN_PLANNING;
import static com.svenruppert.publications.model.EditorialState.IN_PROGRESS;
import static com.svenruppert.publications.model.EditorialState.REVIEW;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("StatusHistory — append-only, ordered by the sequence number")
class StatusHistoryTest {

  @Test
  @DisplayName("fresh history is empty, current() is the initial state")
  void freshHistoryIsEmpty() {
    var h = new StatusHistory<>(BACKLOG);
    assertTrue(h.isEmpty());
    assertEquals(0, h.count());
    assertEquals(BACKLOG, h.current());
    assertEquals(BACKLOG, h.initial());
    assertTrue(h.events().isEmpty());
  }

  @Test
  @DisplayName("record sets current() to the new state")
  void recordSetsCurrent() {
    var h = new StatusHistory<>(BACKLOG);
    h.record(IN_PLANNING, "Sven");
    assertEquals(IN_PLANNING, h.current());
    assertEquals(1, h.count());
  }

  @Test
  @DisplayName("from is derived from current() — a gap-free chain")
  void fromIsDerivedFromCurrent() {
    var h = new StatusHistory<>(BACKLOG);
    h.record(IN_PLANNING, "Sven");
    h.record(IN_PROGRESS, "Editorial");
    h.record(REVIEW, "Guest author");
    h.record(DONE, "Sven");

    List<StatusChange<EditorialState>> e = h.events();
    assertEquals(4, e.size());
    assertEquals(BACKLOG, e.get(0).from());
    for (int i = 1; i < e.size(); i++) {
      assertEquals(e.get(i - 1).to(), e.get(i).from(),
          "from of event " + i + " must be the predecessor's to");
    }
    assertEquals(DONE, h.current());
  }

  @Test
  @DisplayName("sequence is strictly ascending from 0")
  void sequenceIsStrictlyAscendingFromZero() {
    var h = new StatusHistory<>(BACKLOG);
    h.record(IN_PLANNING, "a");
    h.record(IN_PROGRESS, "b");
    h.record(REVIEW, "c");
    List<StatusChange<EditorialState>> e = h.events();
    for (int i = 0; i < e.size(); i++) {
      assertEquals(i, e.get(i).sequence());
    }
  }

  @Test
  @DisplayName("the order rests on the sequence number, not on the timestamp")
  void orderRestsOnSequenceNotTimestamp() {
    var h = new StatusHistory<>(BACKLOG);
    Instant late = Instant.parse("2026-06-10T09:00:00Z");
    Instant early = Instant.parse("2026-05-23T09:00:00Z");
    h.record(IN_PLANNING, "a", late);
    h.record(IN_PROGRESS, "b", early);

    List<StatusChange<EditorialState>> e = h.events();
    assertEquals(0, e.get(0).sequence());
    assertEquals(1, e.get(1).sequence());
    assertEquals(IN_PROGRESS, h.current());
    assertEquals(late, e.get(0).timestamp());
    assertEquals(early, e.get(1).timestamp());
  }

  @Test
  @DisplayName("explicit timestamp is kept (import/test)")
  void explicitTimestampIsKept() {
    var h = new StatusHistory<>(BACKLOG);
    Instant t = Instant.parse("2026-05-23T09:00:00Z");
    h.record(IN_PLANNING, "Sven", t);
    assertEquals(t, h.events().get(0).timestamp());
  }

  @Test
  @DisplayName("events() is an immutable copy")
  void eventsIsImmutableCopy() {
    var h = new StatusHistory<>(BACKLOG);
    h.record(IN_PLANNING, "Sven");
    List<StatusChange<EditorialState>> snapshot = h.events();
    assertThrows(UnsupportedOperationException.class,
        () -> snapshot.add(snapshot.get(0)));
    h.record(IN_PROGRESS, "Sven");
    assertEquals(1, snapshot.size());
    assertEquals(2, h.count());
  }

  @Test
  @DisplayName("constructor rejects a null initial")
  void constructorRejectsNullInitial() {
    assertThrows(NullPointerException.class, () -> new StatusHistory<EditorialState>(null));
  }

  @Test
  @DisplayName("record rejects a null state and a null timestamp")
  void recordRejectsNulls() {
    var h = new StatusHistory<>(BACKLOG);
    assertThrows(NullPointerException.class, () -> h.record(null, "Sven"));
    assertThrows(NullPointerException.class, () -> h.record(IN_PLANNING, "Sven", null));
  }

  @Test
  @DisplayName("actor may be null (own initiative/unknown)")
  void actorMayBeNull() {
    var h = new StatusHistory<>(BACKLOG);
    h.record(IN_PLANNING, null);
    assertEquals(IN_PLANNING, h.current());
    assertFalse(h.isEmpty());
  }

  @Test
  @DisplayName("the history type carries every dimension alike")
  void carriesEveryDimension() {
    var acquisition = new StatusHistory<>(AcquisitionStatus.REQUESTED);
    acquisition.record(AcquisitionStatus.OFFERED, "Sven");
    acquisition.record(AcquisitionStatus.ACCEPTED, "Sven");
    assertEquals(AcquisitionStatus.ACCEPTED, acquisition.current());
    assertEquals(2, acquisition.count());
  }
}
