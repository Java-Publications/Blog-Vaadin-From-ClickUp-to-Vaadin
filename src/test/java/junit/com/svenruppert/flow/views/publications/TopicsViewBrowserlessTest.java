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
import com.svenruppert.flow.views.publications.TopicsView;
import com.svenruppert.jsentinel.authorization.api.SubjectStores;
import com.svenruppert.publications.model.EditorialState;
import com.svenruppert.publications.model.Issue;
import com.svenruppert.publications.model.Part;
import com.svenruppert.publications.model.Tag;
import com.svenruppert.publications.persistence.InMemoryPublicationsPersistence;
import com.svenruppert.publications.persistence.PublicationsProvider;
import com.svenruppert.publications.persistence.PublicationsRepository;
import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.dnd.GridDropMode;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.markdown.Markdown;
import junit.com.svenruppert.flow.TestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("TopicsView (V1) — master issue grid + detail")
class TopicsViewBrowserlessTest extends BrowserlessTest {

  @BeforeEach
  void setUp() {
    TestSupport.seedAdminAndResetBootstrap();
    UI ui = UI.getCurrent();
    if (ui != null) {
      ui.setLocale(Locale.ENGLISH);
    }
    PublicationsRepository repo =
        new PublicationsRepository(new InMemoryPublicationsPersistence());
    repo.createIssue("Blog – Navigation – Coupled navigation");
    Issue testing = repo.createIssue("Blog – Testing – JUnit 5 Extensions");
    testing.addTag(new Tag("JUnit"));
    testing.addPart();
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
  @DisplayName("NAV constant is 'themen'")
  void navConstant() {
    assertEquals("topics", TopicsView.NAV);
  }

  @Test
  @DisplayName("heading is rendered")
  void headingPresent() {
    UI.getCurrent().navigate(TopicsView.class);
    H3 empty = $view(H3.class).first();
    assertEquals("No topic selected", empty.getText());
  }

  @Test
  @DisplayName("master grid lists both seeded issues")
  void masterGridListsIssues() {
    UI.getCurrent().navigate(TopicsView.class);
    Grid<?> grid = $view(Grid.class).first();
    assertEquals(3, grid.getColumns().size(), "title, parts, status columns");
    assertEquals(2, grid.getListDataView().getItemCount());
  }

  @Test
  @DisplayName("linkify wraps bare URLs, leaves existing markdown links untouched (N)")
  void linkifyWrapsBareUrls() {
    assertEquals("see <https://vaadin.com> now",
        TopicsView.linkify("see https://vaadin.com now"));
    assertEquals("[docs](https://vaadin.com/docs)",
        TopicsView.linkify("[docs](https://vaadin.com/docs)"),
        "an existing markdown link must not be double-wrapped");
    assertEquals("go to <https://vaadin.com>.",
        TopicsView.linkify("go to https://vaadin.com."),
        "trailing sentence punctuation stays outside the link");
    assertEquals("", TopicsView.linkify(null));
  }

  @Test
  @DisplayName("a topic whose text has a URL renders it via the Markdown component (N)")
  @SuppressWarnings("unchecked")
  void detailRendersMarkdownForLinks() {
    Issue issue = PublicationsProvider.repository().createIssue("Linky");
    issue.setDescription("Read https://vaadin.com for details.");
    PublicationsProvider.repository().persist();
    UI.getCurrent().navigate(TopicsView.class);
    Grid<Issue> grid = (Grid<Issue>) $view(Grid.class).first();
    grid.select(issue);
    assertTrue($view(Markdown.class).all().size() >= 1,
        "the original text must render via the Markdown component so links are clickable");
  }

  @Test
  @DisplayName("a topic with no parts shows an empty-state + hint and a clear Add part action (O)")
  @SuppressWarnings("unchecked")
  void partsEmptyStateAndHint() {
    Issue issue = PublicationsProvider.repository().createIssue("Empty parts topic");
    UI.getCurrent().navigate(TopicsView.class);
    Grid<Issue> grid = (Grid<Issue>) $view(Grid.class).first();
    grid.select(issue);

    assertTrue($view(H3.class).all().stream().anyMatch(h -> "No parts yet".equals(h.getText())),
        "an empty-state must explain there are no parts");
    assertTrue($view(Span.class).all().stream()
            .anyMatch(s -> s.getText() != null && s.getText().startsWith("A part is a language-neutral")),
        "a hint must explain what a part is");
    assertTrue($view(Button.class).all().stream().map(Button::getText).anyMatch("+ Add part"::equals),
        "a clear Add part action must be present");
  }

  @Test
  @DisplayName("a 'New topic' action sits above the list (P)")
  void createTopicButtonPresent() {
    UI.getCurrent().navigate(TopicsView.class);
    assertTrue($view(Button.class).all().stream()
            .map(Button::getText)
            .collect(Collectors.toList())
            .contains("New topic"),
        "the topic list toolbar must offer a 'New topic' action");
  }

  @Test
  @DisplayName("German locale renders German labels (not the English fallback)")
  void germanLocaleRendersGermanLabels() {
    UI.getCurrent().setLocale(java.util.Locale.GERMAN);
    UI.getCurrent().navigate(TopicsView.class);
    assertEquals("Kein Thema gewählt", $view(com.vaadin.flow.component.html.H3.class).first().getText(),
        "the DE translation bundle must resolve, not fall back to English");
  }

  @Test
  @DisplayName("the topic detail offers an Edit action (G)")
  @SuppressWarnings("unchecked")
  void detailOffersEdit() {
    UI.getCurrent().navigate(TopicsView.class);
    Grid<Issue> grid = (Grid<Issue>) $view(Grid.class).first();
    Issue any = PublicationsProvider.repository().issues().stream().findFirst().orElseThrow();
    grid.select(any);
    assertTrue($view(Button.class).all().stream().map(Button::getText).anyMatch("Edit"::equals),
        "the detail must offer an Edit action for the selected topic");
  }

  @Test
  @DisplayName("an edited topic shows the new title and text in the detail (G)")
  @SuppressWarnings("unchecked")
  void editingUpdatesTitleAndText() {
    Issue issue = PublicationsProvider.repository().createIssue("Before edit");
    // the same mutations the Edit dialog performs on save
    issue.setTitle("After edit");
    issue.setDescription("Freshly edited text.");
    PublicationsProvider.repository().persist();

    UI.getCurrent().navigate(TopicsView.class);
    Grid<Issue> grid = (Grid<Issue>) $view(Grid.class).first();
    grid.select(issue);

    assertTrue($view(com.vaadin.flow.component.html.H3.class).all().stream()
            .anyMatch(h -> "After edit".equals(h.getText())),
        "the detail must show the edited title");
    assertTrue($view(Markdown.class).all().stream()
            .anyMatch(m -> "Freshly edited text.".equals(m.getContent())),
        "the detail must render the edited original text as markdown");
  }

  @Test
  @DisplayName("selecting a topic with an original body renders the original text in the detail")
  @SuppressWarnings("unchecked")
  void detailShowsOriginalText() {
    Issue described = PublicationsProvider.repository().createIssue("Blog – Import – With body");
    described.setDescription("The original ClickUp body text.");
    PublicationsProvider.repository().persist();

    UI.getCurrent().navigate(TopicsView.class);
    Grid<Issue> grid = (Grid<Issue>) $view(Grid.class).first();
    grid.select(described);

    assertTrue($view(Markdown.class).all().stream()
            .anyMatch(m -> "The original ClickUp body text.".equals(m.getContent())),
        "the detail panel must render the selected topic's original text as markdown");
  }

  @Test
  @DisplayName("status filter is multi-select and OR-combines the selected states (AB)")
  @SuppressWarnings("unchecked")
  void statusMultiSelectOr() {
    PublicationsRepository repo = PublicationsProvider.repository();
    repo.createIssue("Alpha done").addPart().changeState(EditorialState.DONE, "t");
    repo.createIssue("Beta review").addPart().changeState(EditorialState.REVIEW, "t");
    repo.persist();

    UI.getCurrent().navigate(TopicsView.class);
    Grid<Issue> grid = (Grid<Issue>) $view(Grid.class).first();

    // The status filter is the EditorialState multi-select (2nd MultiSelectComboBox).
    MultiSelectComboBox<EditorialState> status =
        (MultiSelectComboBox<EditorialState>) $view(MultiSelectComboBox.class).all().get(1);
    status.setValue(Set.of(EditorialState.DONE, EditorialState.REVIEW));
    assertEquals(2, grid.getListDataView().getItemCount(),
        "OR: issues with a part in DONE or REVIEW both match");
    status.setValue(Set.of(EditorialState.DONE));
    assertEquals(1, grid.getListDataView().getItemCount(), "only the DONE issue remains");
  }

  @Test
  @DisplayName("tag filter combines with OR by default and AND when 'All' is selected (AB)")
  @SuppressWarnings("unchecked")
  void tagAndOr() {
    PublicationsRepository repo = PublicationsProvider.repository();
    Issue both = repo.createIssue("Alpha both");
    both.addTag(new Tag("java"));
    both.addTag(new Tag("vaadin"));
    repo.createIssue("Beta java").addTag(new Tag("java"));
    repo.persist();

    UI.getCurrent().navigate(TopicsView.class);
    Grid<Issue> grid = (Grid<Issue>) $view(Grid.class).first();

    // tags = the Tag multi-select (1st MultiSelectComboBox); tag-match = the ComboBox.
    MultiSelectComboBox<Tag> tags =
        (MultiSelectComboBox<Tag>) $view(MultiSelectComboBox.class).all().get(0);
    tags.setValue(Set.of(new Tag("java"), new Tag("vaadin")));
    assertEquals(2, grid.getListDataView().getItemCount(),
        "OR (default): issues carrying any selected tag match");

    // Switch tag-match to All (AND) — selected by the value whose enum name is ALL.
    ComboBox<Object> tagMatch = (ComboBox<Object>) $view(ComboBox.class).first();
    Object all = tagMatch.getListDataView().getItems()
        .filter(m -> "ALL".equals(m.toString())).findFirst().orElseThrow();
    tagMatch.setValue(all);
    assertEquals(1, grid.getListDataView().getItemCount(),
        "AND: only the issue carrying every selected tag matches");
  }

  @Test
  @DisplayName("the local search field filters the master grid by title (AA — no global filter)")
  @SuppressWarnings("unchecked")
  void masterGridHonoursLocalSearch() {
    UI.getCurrent().navigate(TopicsView.class);
    Grid<Issue> grid = (Grid<Issue>) $view(Grid.class).first();
    assertEquals(2, grid.getListDataView().getItemCount(), "both seeded topics before filtering");

    com.vaadin.flow.component.textfield.TextField search =
        $view(com.vaadin.flow.component.textfield.TextField.class).first();
    search.setValue("Testing");
    assertEquals(1, grid.getListDataView().getItemCount(),
        "only the 'Testing' topic must survive the local title search");
    assertEquals("Blog – Testing – JUnit 5 Extensions",
        grid.getListDataView().getItems().findFirst().orElseThrow().title());
  }

  @Test
  @DisplayName("the parts grid is a drop target for reordering rows")
  @SuppressWarnings("unchecked")
  void partsGridSupportsRowReordering() {
    UI.getCurrent().navigate(TopicsView.class);
    Grid<Issue> grid = (Grid<Issue>) $view(Grid.class).first();
    Issue withPart = PublicationsProvider.repository().issues().stream()
        .filter(i -> !i.parts().isEmpty()).findFirst().orElseThrow();
    grid.select(withPart);

    Grid<?> partsGrid = $view(Grid.class).all().stream()
        .filter(g -> "parts-grid".equals(g.getId().orElse(null)))
        .findFirst().orElseThrow(() -> new AssertionError("parts grid not rendered"));
    assertTrue(partsGrid.isRowsDraggable(), "rows must be draggable for reordering");
    assertEquals(GridDropMode.BETWEEN, partsGrid.getDropMode(),
        "the grid must accept drops between rows");
  }

  @Test
  @DisplayName("reorderedParts moves a dragged part below the target and renumbers 1..n")
  void reorderMovesDraggedPartAfterTarget() {
    PublicationsRepository repo =
        new PublicationsRepository(new InMemoryPublicationsPersistence());
    Issue issue = repo.createIssue("Blog – Series – Three parts");
    Part p1 = issue.addPart();
    Part p2 = issue.addPart();
    Part p3 = issue.addPart();

    // Drag p1 below p3 → the exact call the drop handler makes.
    List<Part> newOrder = TopicsView.reorderedParts(issue.partsInOrder(), p1, p3, true);
    assertEquals(List.of(p2, p3, p1), newOrder);

    issue.reorderParts(newOrder);
    assertEquals(List.of(p2, p3, p1), issue.partsInOrder());
    assertEquals(1, p2.position());
    assertEquals(2, p3.position());
    assertEquals(3, p1.position());
  }

  @Test
  @DisplayName("each tag in the selected topic renders a remove (✕) control")
  @SuppressWarnings("unchecked")
  void tagsCanBeRemoved() {
    UI.getCurrent().navigate(TopicsView.class);
    Grid<Issue> grid = (Grid<Issue>) $view(Grid.class).first();
    Issue tagged = PublicationsProvider.repository().issues().stream()
        .filter(i -> !i.tags().isEmpty()).findFirst().orElseThrow();
    grid.select(tagged);
    long removeControls = $view(Button.class).all().stream()
        .filter(b -> {
          String al = b.getElement().getAttribute("aria-label");
          return al != null && al.startsWith("Remove tag");
        }).count();
    assertEquals(tagged.tags().size(), removeControls,
        "one remove control per tag on the selected topic");
  }
}
