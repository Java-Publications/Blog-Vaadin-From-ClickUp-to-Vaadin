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
import com.svenruppert.publications.model.Sprache;
import com.svenruppert.publications.model.Veroeffentlichungsstatus;
import com.svenruppert.publications.model.Vertriebsstatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatusEnumsTest {

  @Test
  void arbeitszustandEndzustaende() {
    assertTrue(Arbeitszustand.DONE.isEndzustand());
    assertTrue(Arbeitszustand.SKIPPED.isEndzustand());
    assertTrue(Arbeitszustand.CANCELLED.isEndzustand());
    assertFalse(Arbeitszustand.BACKLOG.isEndzustand());
    assertFalse(Arbeitszustand.IN_PLANUNG.isEndzustand());
    assertFalse(Arbeitszustand.IN_PROGRESS.isEndzustand());
    assertFalse(Arbeitszustand.REVIEW.isEndzustand());
  }

  @Test
  void vertriebsstatusEndzustaende() {
    assertTrue(Vertriebsstatus.DONE.isEndzustand());
    assertTrue(Vertriebsstatus.SKIPPED.isEndzustand());
    assertTrue(Vertriebsstatus.CANCELLED.isEndzustand());
    assertFalse(Vertriebsstatus.REQUESTED.isEndzustand());
    assertFalse(Vertriebsstatus.OFFERED.isEndzustand());
    assertFalse(Vertriebsstatus.ACCEPTED.isEndzustand());
    assertFalse(Vertriebsstatus.REVIEW.isEndzustand());
  }

  @Test
  void dimensionenHabenDieErwartetenWerte() {
    assertEquals(2, Sprache.values().length);
    assertEquals(4, Veroeffentlichungsstatus.values().length);
    assertEquals(7, Arbeitszustand.values().length);
    assertEquals(7, Vertriebsstatus.values().length);
  }
}
