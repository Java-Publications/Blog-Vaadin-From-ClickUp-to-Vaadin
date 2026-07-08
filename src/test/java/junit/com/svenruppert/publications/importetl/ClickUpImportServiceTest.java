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

package junit.com.svenruppert.publications.importetl;

import com.svenruppert.publications.importetl.ClickUpImportService;
import com.svenruppert.publications.importetl.ImportReport;
import com.svenruppert.publications.model.Arbeitszustand;
import com.svenruppert.publications.model.Issue;
import com.svenruppert.publications.persistence.InMemoryPublicationsPersistence;
import com.svenruppert.publications.persistence.PublicationsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ClickUpImportService — transform + idempotent load")
class ClickUpImportServiceTest {

  private static final String FIXTURE = """
      {"tasks":[
        {"id":"CU-8842","name":"Blog – Navigation – Koppelnavigation",
         "status":{"status":"in progress"},
         "tags":[{"name":"Vaadin"},{"name":"UX"}],"url":"http://x/1"},
        {"id":"CU-8843","name":"Blog – Testing – JUnit 5 Extensions",
         "status":{"status":"complete"},"tags":[]}
      ]}""";

  private ClickUpImportService service;
  private PublicationsRepository repo;

  @BeforeEach
  void setUp() {
    service = new ClickUpImportService();
    repo = new PublicationsRepository(new InMemoryPublicationsPersistence());
  }

  @Test
  @DisplayName("reconstructs the four levels and disentangles the status")
  void transformAndLoadReconstructs() {
    ImportReport report = service.transformAndLoad(FIXTURE, repo);

    assertEquals(2, report.angelegt());
    assertEquals(0, report.uebersprungen());
    assertEquals(2, repo.issues().size());

    Issue koppel = repo.findIssueByHerkunft("CU-8842").orElseThrow();
    assertEquals("Blog – Navigation – Koppelnavigation", koppel.titel());
    assertEquals(2, koppel.tags().size());
    assertEquals(1, koppel.teile().size());
    assertEquals(Arbeitszustand.IN_PROGRESS, koppel.teile().get(0).arbeitszustand());

    Issue testing = repo.findIssueByHerkunft("CU-8843").orElseThrow();
    assertEquals(Arbeitszustand.DONE, testing.teile().get(0).arbeitszustand());

    assertTrue(report.statusVerteilung().containsKey("in progress → IN_PROGRESS"));
    assertTrue(report.statusVerteilung().containsKey("complete → DONE"));
  }

  @Test
  @DisplayName("re-running the same raw data creates no duplicates (idempotent via Herkunft)")
  void reimportIsIdempotent() {
    service.transformAndLoad(FIXTURE, repo);
    ImportReport second = service.transformAndLoad(FIXTURE, repo);

    assertEquals(0, second.angelegt());
    assertEquals(2, second.uebersprungen());
    assertEquals(2, repo.issues().size(), "no duplicates on repeat run");
  }

  @Test
  @DisplayName("status mapping covers the common ClickUp states")
  void statusMapping() {
    assertEquals(Arbeitszustand.IN_PROGRESS, ClickUpImportService.mapStatus("in progress"));
    assertEquals(Arbeitszustand.REVIEW, ClickUpImportService.mapStatus("Review"));
    assertEquals(Arbeitszustand.DONE, ClickUpImportService.mapStatus("complete"));
    assertEquals(Arbeitszustand.CANCELLED, ClickUpImportService.mapStatus("cancelled"));
    assertEquals(Arbeitszustand.BACKLOG, ClickUpImportService.mapStatus("something else"));
    assertEquals(Arbeitszustand.BACKLOG, ClickUpImportService.mapStatus(null));
  }
}
