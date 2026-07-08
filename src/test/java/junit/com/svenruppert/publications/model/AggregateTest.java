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
import com.svenruppert.publications.model.Issue;
import com.svenruppert.publications.model.Sprache;
import com.svenruppert.publications.model.Sprachfassung;
import com.svenruppert.publications.model.Tag;
import com.svenruppert.publications.model.Teil;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AggregateTest {

  @Test
  void issueTitelDarfNichtLeerSein() {
    assertThrows(IllegalArgumentException.class, () -> new Issue("   "));
    assertThrows(NullPointerException.class, () -> new Issue(null));
    assertEquals("Blog – Navigation", new Issue("  Blog – Navigation  ").titel());
  }

  @Test
  void addTeilVergibtAufsteigendeReihenfolgeUndSetztRueckreferenz() {
    Issue issue = new Issue("Blog – Navigation – Koppelnavigation");
    Teil t1 = issue.addTeil();
    Teil t2 = issue.addTeil();
    assertEquals(1, t1.reihenfolge());
    assertEquals(2, t2.reihenfolge());
    assertSame(issue, t1.issue());
    assertEquals(Arbeitszustand.BACKLOG, t1.arbeitszustand());
  }

  @Test
  void tagsWerdenVergebenUndEntfernt() {
    Issue issue = new Issue("Thema");
    issue.addTag(new Tag("Vaadin"));
    issue.addTag(new Tag("UX"));
    assertEquals(2, issue.tags().size());
    issue.removeTag(new Tag("UX"));
    assertEquals(1, issue.tags().size());
    assertTrue(issue.tags().contains(new Tag("Vaadin")));
  }

  @Test
  void ordneTeileNeuVergibtReihenfolgeNeu() {
    Issue issue = new Issue("Thema");
    Teil t1 = issue.addTeil();
    Teil t2 = issue.addTeil();
    Teil t3 = issue.addTeil();

    issue.ordneTeileNeu(List.of(t3, t1, t2));

    assertEquals(1, t3.reihenfolge());
    assertEquals(2, t1.reihenfolge());
    assertEquals(3, t2.reihenfolge());
    assertEquals(List.of(t3, t1, t2), issue.teileInReihenfolge());
  }

  @Test
  void ordneTeileNeuLehntFremdeMengeAb() {
    Issue issue = new Issue("Thema");
    Teil t1 = issue.addTeil();
    issue.addTeil();
    assertThrows(IllegalArgumentException.class, () -> issue.ordneTeileNeu(List.of(t1)));
  }

  @Test
  void hoechstensEineSprachfassungJeSprache() {
    Teil teil = new Issue("Thema").addTeil();
    Sprachfassung de = teil.addSprachfassung(Sprache.DEUTSCH);
    assertEquals(Sprache.DEUTSCH, de.sprache());
    assertThrows(IllegalArgumentException.class, () -> teil.addSprachfassung(Sprache.DEUTSCH));
    teil.addSprachfassung(Sprache.ENGLISCH);
    assertEquals(2, teil.sprachfassungen().size());
    assertTrue(teil.fassung(Sprache.ENGLISCH).isPresent());
  }

  @Test
  void arbeitszustandWirdUeberVerlaufFortgeschrieben() {
    Teil teil = new Issue("Thema").addTeil();
    teil.wechsleZustand(Arbeitszustand.IN_PLANUNG, "Sven");
    teil.wechsleZustand(Arbeitszustand.IN_PROGRESS, "Sven");
    assertEquals(Arbeitszustand.IN_PROGRESS, teil.arbeitszustand());
    assertEquals(2, teil.arbeit().anzahl());
  }

  @Test
  void geplanteZeichenLehntNegativAb() {
    Sprachfassung f = new Issue("T").addTeil().addSprachfassung(Sprache.DEUTSCH);
    f.setGeplanteZeichen(9000);
    assertEquals(9000, f.geplanteZeichen());
    assertThrows(IllegalArgumentException.class, () -> f.setGeplanteZeichen(-1));
  }
}
