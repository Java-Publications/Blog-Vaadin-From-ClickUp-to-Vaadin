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

package junit.com.svenruppert.publications.persistence;

import com.svenruppert.publications.model.Arbeitszustand;
import com.svenruppert.publications.model.Issue;
import com.svenruppert.publications.model.Publikationsort;
import com.svenruppert.publications.model.Sprache;
import com.svenruppert.publications.model.Sprachfassung;
import com.svenruppert.publications.model.Tag;
import com.svenruppert.publications.model.Teil;
import com.svenruppert.publications.model.Veroeffentlichung;
import com.svenruppert.publications.model.Veroeffentlichungsstatus;
import com.svenruppert.publications.model.Vertriebsstatus;
import com.svenruppert.publications.persistence.EclipseStorePublicationsPersistence;
import com.svenruppert.publications.persistence.PublicationsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * No-Mocks-Round-Trip gegen eine echte Eclipse-Store-Instanz in einem
 * Temp-Verzeichnis: schreiben, schließen, in einer frischen Instanz wieder
 * öffnen und den kompletten Graph inklusive der Statushistorien prüfen.
 */
class EclipseStorePublicationsPersistenceTest {

  @Test
  void graphUeberlebtNeustart(@TempDir Path tmp) {
    Path dir = tmp.resolve("publications");

    var p1 = new EclipseStorePublicationsPersistence(dir);
    var repo1 = new PublicationsRepository(p1);
    Publikationsort ort =
        repo1.neuerPublikationsort("svenruppert.com", Set.of(Sprache.DEUTSCH, Sprache.ENGLISCH));
    Issue issue = repo1.neuesIssue("Blog – Navigation – Koppelnavigation");
    issue.setHerkunft("CU-8842");
    issue.addTag(new Tag("Vaadin"));
    Teil teil = issue.addTeil();
    teil.wechsleZustand(Arbeitszustand.IN_PLANUNG, "Sven");
    teil.wechsleZustand(Arbeitszustand.IN_PROGRESS, "Redaktion");
    Sprachfassung de = teil.addSprachfassung(Sprache.DEUTSCH);
    de.setManuskript("docs.google.com/…/koppelnav-de");
    de.setGeplanteZeichen(9000);
    Veroeffentlichung v = de.planeVeroeffentlichung(ort);
    v.wechsleStatus(Veroeffentlichungsstatus.PREPARED, "Sven");
    v.wechsleVertrieb(Vertriebsstatus.ACCEPTED, "Sven");
    repo1.persist();
    p1.close();

    // frische Instanz, gleiches Verzeichnis
    var p2 = new EclipseStorePublicationsPersistence(dir);
    var repo2 = new PublicationsRepository(p2);
    try {
      assertEquals(1, repo2.issues().size());
      Issue loaded = repo2.issues().get(0);
      assertEquals("Blog – Navigation – Koppelnavigation", loaded.titel());
      assertEquals("CU-8842", loaded.herkunft());
      assertEquals(Set.of(new Tag("Vaadin")), loaded.tags());

      assertEquals(1, loaded.teile().size());
      Teil lt = loaded.teile().get(0);
      assertEquals(Arbeitszustand.IN_PROGRESS, lt.arbeitszustand());
      assertEquals(2, lt.arbeit().anzahl(), "die Statushistorie muss den Neustart überleben");

      Sprachfassung lf = lt.fassung(Sprache.DEUTSCH).orElseThrow();
      assertEquals(9000, lf.geplanteZeichen());

      Veroeffentlichung lv = lf.veroeffentlichungen().get(0);
      assertEquals(Veroeffentlichungsstatus.PREPARED, lv.herstellungsstatus());
      assertEquals(Vertriebsstatus.ACCEPTED, lv.akquisestatus());
      assertEquals(ort.id(), lv.ort().id());

      assertEquals(1, repo2.publikationsorte().size());
    } finally {
      p2.close();
    }
  }
}
