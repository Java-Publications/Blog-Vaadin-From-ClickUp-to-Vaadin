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

package junit.com.svenruppert.flow.views.publications;

import com.svenruppert.flow.security.model.AppUser;
import com.svenruppert.flow.security.roles.AuthorizationRole;
import com.svenruppert.flow.views.publications.HistoryView;
import com.svenruppert.jsentinel.authorization.api.SubjectStores;
import com.svenruppert.publications.model.EditorialState;
import com.svenruppert.publications.model.Issue;
import com.svenruppert.publications.model.Part;
import com.svenruppert.publications.persistence.InMemoryPublicationsPersistence;
import com.svenruppert.publications.persistence.PublicationsProvider;
import com.svenruppert.publications.persistence.PublicationsRepository;
import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import junit.com.svenruppert.flow.TestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Locale;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("HistoryView (V5) — append-only history, ordered by sequence")
class HistoryViewBrowserlessTest extends BrowserlessTest {

  private UUID partId;

  @BeforeEach
  void setUp() {
    TestSupport.seedAdminAndResetBootstrap();
    UI ui = UI.getCurrent();
    if (ui != null) {
      ui.setLocale(Locale.ENGLISH);
    }
    PublicationsRepository repo =
        new PublicationsRepository(new InMemoryPublicationsPersistence());
    Issue issue = repo.createIssue("Blog – Navigation – Coupled navigation");
    Part part = issue.addPart();
    part.changeState(EditorialState.IN_PLANNING, "Sven");
    part.changeState(EditorialState.IN_PROGRESS, "Editorial");
    repo.persist();
    partId = part.id();
    PublicationsProvider.setRepository(repo);

    SubjectStores.subjectStore().setCurrentSubject(
        new AppUser(1L, "Alice", EnumSet.of(AuthorizationRole.ADMIN, AuthorizationRole.USER)),
        AppUser.class);
  }

  @AfterEach
  void tearDown() {
    PublicationsProvider.reset();
    SubjectStores.subjectStore().deleteCurrentSubject(AppUser.class);
  }

  @Test
  @DisplayName("part history lists every status change")
  void listsPartHistory() {
    UI.getCurrent().navigate(HistoryView.class, "teil/" + partId);
    assertTrue($view(H1.class).first().getText().startsWith("History"));
    Grid<?> grid = $view(Grid.class).first();
    assertEquals(5, grid.getColumns().size(), "Seq, From, To, Actor, Timestamp");
    assertEquals(2, grid.getListDataView().getItemCount());
  }

  @Test
  @DisplayName("back from a part's history returns to the topic workspace, not the version editor (V)")
  void partHistoryBackGoesToTopics() {
    UI.getCurrent().navigate(HistoryView.class, "teil/" + partId);
    com.vaadin.flow.component.button.Button back = $view(com.vaadin.flow.component.button.Button.class)
        .all().stream().filter(b -> "Back".equals(b.getText())).findFirst()
        .orElseThrow(() -> new AssertionError("no back button on the part history"));

    back.click();
    // Clicking back must land on the topic workspace (route 'themen'), not 'teil'.
    assertTrue($view(H1.class).all().stream()
            .anyMatch(h -> "Topic workspace".equals(h.getText())),
        "back from a part history must return to the topic workspace");
  }

  @Test
  @DisplayName("no path → empty state")
  void emptyWithoutPath() {
    UI.getCurrent().navigate(HistoryView.class);
    assertEquals("No history selected", $view(H3.class).first().getText());
  }
}
