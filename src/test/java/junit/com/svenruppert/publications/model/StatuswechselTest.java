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

import com.svenruppert.publications.model.Arbeitszustand;
import com.svenruppert.publications.model.Statuswechsel;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StatuswechselTest {

  private static final Instant T = Instant.parse("2026-05-23T09:00:00Z");

  @Test
  void gueltigesEreignisWirdAkzeptiert() {
    var w = new Statuswechsel<>(0L, Arbeitszustand.BACKLOG, Arbeitszustand.IN_PLANUNG, "Sven", T);
    assertEquals(0L, w.folge());
    assertEquals(Arbeitszustand.BACKLOG, w.von());
    assertEquals(Arbeitszustand.IN_PLANUNG, w.nach());
    assertEquals("Sven", w.akteur());
    assertEquals(T, w.zeitpunkt());
  }

  @Test
  void negativeFolgeWirdAbgelehnt() {
    assertThrows(IllegalArgumentException.class,
        () -> new Statuswechsel<>(-1L, Arbeitszustand.BACKLOG, Arbeitszustand.DONE, "x", T));
  }

  @Test
  void nullFelderWerdenAbgelehnt() {
    assertThrows(NullPointerException.class,
        () -> new Statuswechsel<>(0L, null, Arbeitszustand.DONE, "x", T));
    assertThrows(NullPointerException.class,
        () -> new Statuswechsel<>(0L, Arbeitszustand.BACKLOG, null, "x", T));
    assertThrows(NullPointerException.class,
        () -> new Statuswechsel<>(0L, Arbeitszustand.BACKLOG, Arbeitszustand.DONE, "x", null));
  }

  @Test
  void akteurDarfNullSein() {
    assertDoesNotThrow(
        () -> new Statuswechsel<>(0L, Arbeitszustand.BACKLOG, Arbeitszustand.DONE, null, T));
  }
}
