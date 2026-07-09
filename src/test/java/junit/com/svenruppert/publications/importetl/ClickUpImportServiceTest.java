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
import com.svenruppert.publications.model.EditorialState;
import com.svenruppert.publications.model.Issue;
import com.svenruppert.publications.persistence.InMemoryPublicationsPersistence;
import com.svenruppert.publications.persistence.PublicationsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ClickUpImportService — transform + idempotent load")
class ClickUpImportServiceTest {

  private static final String FIXTURE = """
      {"tasks":[
        {"id":"CU-8842","name":"Blog – Navigation – Coupled navigation",
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

    assertEquals(2, report.created());
    assertEquals(0, report.skipped());
    assertEquals(2, repo.issues().size());

    Issue coupled = repo.findIssueByOrigin("CU-8842").orElseThrow();
    assertEquals("Blog – Navigation – Coupled navigation", coupled.title());
    assertEquals(2, coupled.tags().size());
    assertEquals(1, coupled.parts().size());
    assertEquals(EditorialState.IN_PROGRESS, coupled.parts().get(0).editorialState());

    Issue testing = repo.findIssueByOrigin("CU-8843").orElseThrow();
    assertEquals(EditorialState.DONE, testing.parts().get(0).editorialState());

    assertTrue(report.statusDistribution().containsKey("in progress → IN_PROGRESS"));
    assertTrue(report.statusDistribution().containsKey("complete → DONE"));
  }

  @Test
  @DisplayName("re-running the same raw data creates no duplicates (idempotent via origin)")
  void reimportIsIdempotent() {
    service.transformAndLoad(FIXTURE, repo);
    ImportReport second = service.transformAndLoad(FIXTURE, repo);

    assertEquals(0, second.created());
    assertEquals(2, second.skipped());
    assertEquals(2, repo.issues().size(), "no duplicates on repeat run");
  }

  @Test
  @DisplayName("progress listener is notified once per task, ending at done == total")
  void reportsProgress() {
    List<int[]> ticks = new ArrayList<>();
    service.transformAndLoad(FIXTURE, repo, (done, total) -> ticks.add(new int[]{done, total}));

    assertEquals(2, ticks.size(), "one tick per task");
    assertEquals(1, ticks.get(0)[0]);
    assertEquals(2, ticks.get(0)[1]);
    assertEquals(2, ticks.get(1)[0], "final tick reaches the total");
    assertEquals(2, ticks.get(1)[1]);
  }

  @Test
  @DisplayName("the original ClickUp text is captured onto the imported issue")
  void capturesOriginalText() {
    String withText = """
        {"tasks":[
          {"id":"CU-9001","name":"Blog – Import – Original text",
           "status":{"status":"planned"},"tags":[],
           "text_content":"This is the original body from ClickUp.",
           "description":"# markdown ignored when text_content is present"}
        ]}""";
    service.transformAndLoad(withText, repo);

    Issue issue = repo.findIssueByOrigin("CU-9001").orElseThrow();
    assertNotNull(issue.description(), "the original text must be captured");
    assertEquals("This is the original body from ClickUp.", issue.description());
  }

  @Test
  @DisplayName("status mapping covers the common ClickUp states")
  void statusMapping() {
    assertEquals(EditorialState.IN_PROGRESS, ClickUpImportService.mapStatus("in progress"));
    assertEquals(EditorialState.REVIEW, ClickUpImportService.mapStatus("Review"));
    assertEquals(EditorialState.DONE, ClickUpImportService.mapStatus("complete"));
    assertEquals(EditorialState.CANCELLED, ClickUpImportService.mapStatus("cancelled"));
    assertEquals(EditorialState.BACKLOG, ClickUpImportService.mapStatus("something else"));
    assertEquals(EditorialState.BACKLOG, ClickUpImportService.mapStatus(null));
  }
}
