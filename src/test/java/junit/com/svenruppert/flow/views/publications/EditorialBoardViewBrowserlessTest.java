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
import com.svenruppert.flow.views.publications.EditorialBoardView;
import com.svenruppert.jsentinel.authorization.api.SubjectStores;
import com.svenruppert.publications.model.EditorialState;
import com.svenruppert.publications.model.Issue;
import com.svenruppert.publications.model.Part;
import com.svenruppert.publications.persistence.InMemoryPublicationsPersistence;
import com.svenruppert.publications.persistence.PublicationsProvider;
import com.svenruppert.publications.persistence.PublicationsRepository;
import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dnd.DragStartEvent;
import com.vaadin.flow.component.dnd.DropEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.select.Select;
import junit.com.svenruppert.flow.TestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("EditorialBoardView (V3) — columns per state + drag&drop")
class EditorialBoardViewBrowserlessTest extends BrowserlessTest {

  private Part p1;
  private Part p2;

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
    p1 = issue.addPart();
    p2 = issue.addPart();
    p2.changeState(EditorialState.IN_PROGRESS, "Sven");
    repo.persist();
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
  @DisplayName("NAV is 'redaktion' and each part gets a move-select card")
  void boardRendersCards() {
    assertEquals("redaktion", EditorialBoardView.NAV);
    UI.getCurrent().navigate(EditorialBoardView.class);
    assertEquals("Editorial board", $view(H1.class).first().getText());
    assertEquals(2, $view(Select.class).all().size(),
        "one move-Select per part card (two seeded parts)");
  }

  @Test
  @DisplayName("dragging a BACKLOG card onto the IN_PROGRESS column advances the part's state")
  void dropAdvancesEditorialState() {
    UI.getCurrent().navigate(EditorialBoardView.class);
    assertEquals(EditorialState.BACKLOG, p1.editorialState(), "precondition: p1 starts in BACKLOG");

    Div card = byId("card-" + p1.id());
    Div column = byId("col-" + EditorialState.IN_PROGRESS.name());

    // Simulate the client's drag&drop round-trip: drag start on the card marks the
    // dragged part, the drop on the target column fires the state advance.
    ComponentUtil.fireEvent(card, new DragStartEvent<>(card, true));
    ComponentUtil.fireEvent(column, new DropEvent<>(column, true, "move"));

    assertEquals(EditorialState.IN_PROGRESS, p1.editorialState(),
        "the drop must advance the part to the target column's state");
    // The already-IN_PROGRESS part is untouched.
    assertEquals(EditorialState.IN_PROGRESS, p2.editorialState());
  }

  private Div byId(String id) {
    return $view(Div.class).all().stream()
        .filter(d -> id.equals(d.getId().orElse(null)))
        .findFirst()
        .orElseThrow(() -> new AssertionError("no Div with id " + id + " in the board"));
  }
}
