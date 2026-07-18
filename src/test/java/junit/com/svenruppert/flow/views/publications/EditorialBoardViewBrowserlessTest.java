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
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dnd.DragStartEvent;
import com.vaadin.flow.component.dnd.DropEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.treegrid.TreeGrid;
import junit.com.svenruppert.flow.TestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("EditorialBoardView (V3) — board + table, drag&drop")
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

    // The board honours the session-scoped global filter (F6); keep it empty so
    // the seeded issue is visible regardless of test order.
    com.svenruppert.flow.views.publications.PublicationsFilter f =
        com.svenruppert.flow.views.publications.PublicationsFilter.current();
    f.setTitleQuery("");
    f.setState(null);
  }

  @AfterEach
  void tearDown() {
    PublicationsProvider.reset();
    SubjectStores.subjectStore().deleteCurrentSubject(AppUser.class);
  }

  @Test
  @DisplayName("NAV is 'redaktion'; cards are drag handles with no redundant state select (F)")
  void boardRendersCards() {
    assertEquals("redaktion", EditorialBoardView.NAV);
    UI.getCurrent().navigate(EditorialBoardView.class);
    assertEquals("Editorial board", $view(H1.class).first().getText());
    assertEquals(0, $view(Select.class).all().size(),
        "the per-card state Select was removed — the column already denotes the state");
    long cards = $view(Div.class).all().stream()
        .filter(d -> d.getId().orElse("").startsWith("card-")).count();
    assertEquals(2, cards, "two seeded parts render as two draggable cards");
  }

  @Test
  @DisplayName("switching to the Table tab reveals a TreeGrid grouped by status (C)")
  void tableViewPresent() {
    UI.getCurrent().navigate(EditorialBoardView.class);
    // The table is hidden until its tab is selected; $view only sees visible ones.
    com.vaadin.flow.component.tabs.Tabs tabs =
        $view(com.vaadin.flow.component.tabs.Tabs.class).first();
    tabs.setSelectedIndex(1);
    assertEquals(1, $view(TreeGrid.class).all().size(),
        "a grouped-by-status table (TreeGrid) becomes visible on the Table tab");
  }

  @Test
  @DisplayName("partsInStateSortedByName groups a state's parts sorted by topic name (C)")
  void tableSortsByName() {
    PublicationsRepository r =
        new PublicationsRepository(new InMemoryPublicationsPersistence());
    Part bravo = r.createIssue("Bravo topic").addPart();
    Part alpha = r.createIssue("Alpha topic").addPart();
    Part charlie = r.createIssue("Charlie topic").addPart();

    List<Part> sorted = EditorialBoardView.partsInStateSortedByName(
        List.of(bravo, alpha, charlie), EditorialState.BACKLOG);

    assertEquals(List.of(alpha, bravo, charlie), sorted,
        "within a status, parts are ordered by topic name A→C");
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

  @Test
  @DisplayName("double-clicking a card runs the editor handler for the part without error (J)")
  void doubleClickWiredToEditor() {
    UI.getCurrent().navigate(EditorialBoardView.class);
    Div card = byId("card-" + p1.id());

    // Firing the card's double-click must run the handler that opens the shared
    // TopicEditDialog for the part's issue — exercised here end-to-end (the opened
    // overlay itself is not browserless-observable; the dialog is unit-tested in
    // TopicEditDialogTest).
    assertDoesNotThrow(() -> ComponentUtil.fireEvent(card, new ClickEvent<>(card)));
  }

  @Test
  @DisplayName("the board free-text search narrows the visible cards (W)")
  void searchNarrowsBoard() {
    UI.getCurrent().navigate(EditorialBoardView.class);
    assertEquals(2, cards(), "two seeded parts render as two cards");

    com.vaadin.flow.component.textfield.TextField search =
        $view(com.vaadin.flow.component.textfield.TextField.class).first();
    search.setValue("Navigation");           // matches the seeded topic title
    assertEquals(2, cards(), "a matching search keeps the cards");

    search.setValue("no-such-topic");
    assertEquals(0, cards(), "a non-matching search removes all cards");
  }

  @Test
  @DisplayName("the state filter limits the board to the selected states (W)")
  @SuppressWarnings("unchecked")
  void stateFilterLimitsColumns() {
    UI.getCurrent().navigate(EditorialBoardView.class);
    assertEquals(EditorialState.values().length, columns(), "all states shown by default");

    // The board has two multi-selects (tags, then state); the state filter is second.
    com.vaadin.flow.component.combobox.MultiSelectComboBox<EditorialState> stateFilter =
        (com.vaadin.flow.component.combobox.MultiSelectComboBox<EditorialState>)
            $view(com.vaadin.flow.component.combobox.MultiSelectComboBox.class).all().get(1);
    stateFilter.setValue(java.util.Set.of(EditorialState.IN_PROGRESS));
    assertEquals(1, columns(), "only the selected state's column remains");
  }

  private long cards() {
    return $view(Div.class).all().stream()
        .filter(d -> d.getId().orElse("").startsWith("card-")).count();
  }

  private long columns() {
    return $view(Div.class).all().stream()
        .filter(d -> d.getId().orElse("").startsWith("col-")).count();
  }

  private Div byId(String id) {
    return $view(Div.class).all().stream()
        .filter(d -> id.equals(d.getId().orElse(null)))
        .findFirst()
        .orElseThrow(() -> new AssertionError("no Div with id " + id + " in the board"));
  }

}
