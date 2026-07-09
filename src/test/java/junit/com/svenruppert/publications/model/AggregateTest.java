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
import com.svenruppert.publications.model.Issue;
import com.svenruppert.publications.model.Language;
import com.svenruppert.publications.model.LanguageVersion;
import com.svenruppert.publications.model.Part;
import com.svenruppert.publications.model.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AggregateTest {

  @Test
  void issueTitleMustNotBeBlank() {
    assertThrows(IllegalArgumentException.class, () -> new Issue("   "));
    assertThrows(NullPointerException.class, () -> new Issue(null));
    assertEquals("Blog – Navigation", new Issue("  Blog – Navigation  ").title());
  }

  @Test
  void addPartAssignsAscendingPositionAndSetsBackReference() {
    Issue issue = new Issue("Blog – Navigation – Coupled navigation");
    Part p1 = issue.addPart();
    Part p2 = issue.addPart();
    assertEquals(1, p1.position());
    assertEquals(2, p2.position());
    assertSame(issue, p1.issue());
    assertEquals(EditorialState.BACKLOG, p1.editorialState());
  }

  @Test
  void tagsAreAddedAndRemoved() {
    Issue issue = new Issue("Topic");
    issue.addTag(new Tag("Vaadin"));
    issue.addTag(new Tag("UX"));
    assertEquals(2, issue.tags().size());
    issue.removeTag(new Tag("UX"));
    assertEquals(1, issue.tags().size());
    assertTrue(issue.tags().contains(new Tag("Vaadin")));
  }

  @Test
  void reorderPartsReassignsPositions() {
    Issue issue = new Issue("Topic");
    Part p1 = issue.addPart();
    Part p2 = issue.addPart();
    Part p3 = issue.addPart();

    issue.reorderParts(List.of(p3, p1, p2));

    assertEquals(1, p3.position());
    assertEquals(2, p1.position());
    assertEquals(3, p2.position());
    assertEquals(List.of(p3, p1, p2), issue.partsInOrder());
  }

  @Test
  void reorderPartsRejectsForeignSet() {
    Issue issue = new Issue("Topic");
    Part p1 = issue.addPart();
    issue.addPart();
    assertThrows(IllegalArgumentException.class, () -> issue.reorderParts(List.of(p1)));
  }

  @Test
  void atMostOneLanguageVersionPerLanguage() {
    Part part = new Issue("Topic").addPart();
    LanguageVersion de = part.addLanguageVersion(Language.GERMAN);
    assertEquals(Language.GERMAN, de.language());
    assertThrows(IllegalArgumentException.class, () -> part.addLanguageVersion(Language.GERMAN));
    part.addLanguageVersion(Language.ENGLISH);
    assertEquals(2, part.languageVersions().size());
    assertTrue(part.versionFor(Language.ENGLISH).isPresent());
  }

  @Test
  void editorialStateIsAdvancedThroughTheHistory() {
    Part part = new Issue("Topic").addPart();
    part.changeState(EditorialState.IN_PLANNING, "Sven");
    part.changeState(EditorialState.IN_PROGRESS, "Sven");
    assertEquals(EditorialState.IN_PROGRESS, part.editorialState());
    assertEquals(2, part.editorialWork().count());
  }

  @Test
  void plannedCharactersRejectsNegative() {
    LanguageVersion v = new Issue("T").addPart().addLanguageVersion(Language.GERMAN);
    v.setPlannedCharacters(9000);
    assertEquals(9000, v.plannedCharacters());
    assertThrows(IllegalArgumentException.class, () -> v.setPlannedCharacters(-1));
  }
}
