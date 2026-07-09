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
import com.svenruppert.publications.model.Language;
import com.svenruppert.publications.model.ProductionStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatusEnumsTest {

  @Test
  void editorialTerminalStates() {
    assertTrue(EditorialState.DONE.isTerminal());
    assertTrue(EditorialState.SKIPPED.isTerminal());
    assertTrue(EditorialState.CANCELLED.isTerminal());
    assertFalse(EditorialState.BACKLOG.isTerminal());
    assertFalse(EditorialState.IN_PLANNING.isTerminal());
    assertFalse(EditorialState.IN_PROGRESS.isTerminal());
    assertFalse(EditorialState.REVIEW.isTerminal());
  }

  @Test
  void acquisitionTerminalStates() {
    assertTrue(AcquisitionStatus.DONE.isTerminal());
    assertTrue(AcquisitionStatus.SKIPPED.isTerminal());
    assertTrue(AcquisitionStatus.CANCELLED.isTerminal());
    assertFalse(AcquisitionStatus.REQUESTED.isTerminal());
    assertFalse(AcquisitionStatus.OFFERED.isTerminal());
    assertFalse(AcquisitionStatus.ACCEPTED.isTerminal());
    assertFalse(AcquisitionStatus.REVIEW.isTerminal());
  }

  @Test
  void dimensionsHaveTheExpectedValues() {
    assertEquals(2, Language.values().length);
    assertEquals(4, ProductionStatus.values().length);
    assertEquals(7, EditorialState.values().length);
    assertEquals(7, AcquisitionStatus.values().length);
  }
}
