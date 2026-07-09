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

import com.svenruppert.publications.model.AcquisitionStatus;
import com.svenruppert.publications.model.EditorialState;
import com.svenruppert.publications.model.Issue;
import com.svenruppert.publications.model.Language;
import com.svenruppert.publications.model.LanguageVersion;
import com.svenruppert.publications.model.Part;
import com.svenruppert.publications.model.ProductionStatus;
import com.svenruppert.publications.model.Publication;
import com.svenruppert.publications.model.PublicationPlace;
import com.svenruppert.publications.model.Tag;
import com.svenruppert.publications.persistence.EclipseStorePublicationsPersistence;
import com.svenruppert.publications.persistence.PublicationsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * No-mocks round-trip against a real Eclipse-Store instance in a temp directory:
 * write, close, reopen in a fresh instance and check the whole graph including the
 * status histories.
 */
class EclipseStorePublicationsPersistenceTest {

  @Test
  void graphSurvivesRestart(@TempDir Path tmp) {
    Path dir = tmp.resolve("publications");

    var p1 = new EclipseStorePublicationsPersistence(dir);
    var repo1 = new PublicationsRepository(p1);
    PublicationPlace place =
        repo1.createPublicationPlace("svenruppert.com", Set.of(Language.GERMAN, Language.ENGLISH));
    Issue issue = repo1.createIssue("Blog – Navigation – Coupled navigation");
    issue.setOrigin("CU-8842");
    issue.addTag(new Tag("Vaadin"));
    Part part = issue.addPart();
    part.changeState(EditorialState.IN_PLANNING, "Sven");
    part.changeState(EditorialState.IN_PROGRESS, "Editorial");
    LanguageVersion de = part.addLanguageVersion(Language.GERMAN);
    de.setManuscript("docs.google.com/…/coupled-nav-de");
    de.setPlannedCharacters(9000);
    Publication v = de.planPublication(place);
    v.changeProductionStatus(ProductionStatus.PREPARED, "Sven");
    v.changeAcquisitionStatus(AcquisitionStatus.ACCEPTED, "Sven");
    repo1.persist();
    p1.close();

    // fresh instance, same directory
    var p2 = new EclipseStorePublicationsPersistence(dir);
    var repo2 = new PublicationsRepository(p2);
    try {
      assertEquals(1, repo2.issues().size());
      Issue loaded = repo2.issues().get(0);
      assertEquals("Blog – Navigation – Coupled navigation", loaded.title());
      assertEquals("CU-8842", loaded.origin());
      assertEquals(Set.of(new Tag("Vaadin")), loaded.tags());

      assertEquals(1, loaded.parts().size());
      Part lp = loaded.parts().get(0);
      assertEquals(EditorialState.IN_PROGRESS, lp.editorialState());
      assertEquals(2, lp.editorialWork().count(), "the status history must survive the restart");

      LanguageVersion lv = lp.versionFor(Language.GERMAN).orElseThrow();
      assertEquals(9000, lv.plannedCharacters());

      Publication lpub = lv.publications().get(0);
      assertEquals(ProductionStatus.PREPARED, lpub.productionStatus());
      assertEquals(AcquisitionStatus.ACCEPTED, lpub.acquisitionStatus());
      assertEquals(place.id(), lpub.place().id());

      assertEquals(1, repo2.publicationPlaces().size());
    } finally {
      p2.close();
    }
  }
}
