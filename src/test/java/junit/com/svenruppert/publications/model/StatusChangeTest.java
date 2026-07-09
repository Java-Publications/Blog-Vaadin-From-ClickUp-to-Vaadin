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

import com.svenruppert.publications.model.EditorialState;
import com.svenruppert.publications.model.StatusChange;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StatusChangeTest {

  private static final Instant T = Instant.parse("2026-05-23T09:00:00Z");

  @Test
  void validEventIsAccepted() {
    var c = new StatusChange<>(0L, EditorialState.BACKLOG, EditorialState.IN_PLANNING, "Sven", T);
    assertEquals(0L, c.sequence());
    assertEquals(EditorialState.BACKLOG, c.from());
    assertEquals(EditorialState.IN_PLANNING, c.to());
    assertEquals("Sven", c.actor());
    assertEquals(T, c.timestamp());
  }

  @Test
  void negativeSequenceIsRejected() {
    assertThrows(IllegalArgumentException.class,
        () -> new StatusChange<>(-1L, EditorialState.BACKLOG, EditorialState.DONE, "x", T));
  }

  @Test
  void nullFieldsAreRejected() {
    assertThrows(NullPointerException.class,
        () -> new StatusChange<>(0L, null, EditorialState.DONE, "x", T));
    assertThrows(NullPointerException.class,
        () -> new StatusChange<>(0L, EditorialState.BACKLOG, null, "x", T));
    assertThrows(NullPointerException.class,
        () -> new StatusChange<>(0L, EditorialState.BACKLOG, EditorialState.DONE, "x", null));
  }

  @Test
  void actorMayBeNull() {
    assertDoesNotThrow(
        () -> new StatusChange<>(0L, EditorialState.BACKLOG, EditorialState.DONE, null, T));
  }
}
