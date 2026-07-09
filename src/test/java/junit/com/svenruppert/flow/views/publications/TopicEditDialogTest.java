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

import com.svenruppert.flow.views.publications.TopicEditDialog;
import com.svenruppert.publications.model.EditorialState;
import com.svenruppert.publications.model.Issue;
import com.svenruppert.publications.model.Part;
import com.svenruppert.publications.persistence.InMemoryPublicationsPersistence;
import com.svenruppert.publications.persistence.PublicationsRepository;
import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.markdown.Markdown;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextArea;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("TopicEditDialog — resizable markdown editor + status control")
class TopicEditDialogTest extends BrowserlessTest {

  private PublicationsRepository repo;

  @BeforeEach
  void setUp() {
    repo = new PublicationsRepository(new InMemoryPublicationsPersistence());
  }

  @Test
  @DisplayName("save applies the edited title and original text, and runs the callback")
  void savesTitleAndText() {
    Issue issue = repo.createIssue("Old title");
    AtomicBoolean refreshed = new AtomicBoolean(false);
    TopicEditDialog dialog = new TopicEditDialog(issue, null, repo, () -> refreshed.set(true));

    assertTrue(dialog.save("New title", "New **body** from the editor", null));

    assertEquals("New title", issue.title());
    assertEquals("New **body** from the editor", issue.description());
    assertTrue(refreshed.get(), "the onSaved callback must run after a successful save");
  }

  @Test
  @DisplayName("a blank title is rejected and leaves the topic unchanged")
  void blankTitleRejected() {
    Issue issue = repo.createIssue("Keep me");
    TopicEditDialog dialog = new TopicEditDialog(issue, null, repo, () -> { });

    assertFalse(dialog.save("   ", "whatever", null));
    assertEquals("Keep me", issue.title());
  }

  @Test
  @DisplayName("for a part, save changes the editorial status (M)")
  void savesEditorialStatusForPart() {
    Issue issue = repo.createIssue("Topic");
    Part part = issue.addPart();
    TopicEditDialog dialog = new TopicEditDialog(issue, part, repo, () -> { });

    assertTrue(dialog.save("Topic", "body", EditorialState.IN_PROGRESS));
    assertEquals(EditorialState.IN_PROGRESS, part.editorialState());
  }

  @Test
  @DisplayName("open() marks the dialog opened — the editor the board/detail launches on double-click/edit")
  void opens() {
    TopicEditDialog dialog = new TopicEditDialog(repo.createIssue("T"), null, repo, () -> { });
    assertFalse(dialog.isOpened());
    dialog.open();
    assertTrue(dialog.isOpened(), "the editor must open");
  }

  @Test
  @DisplayName("the dialog is width-resizable and carries a Visual/Syntax markdown editor (H, I)")
  void resizableMarkdownEditor() {
    TopicEditDialog dialog = new TopicEditDialog(repo.createIssue("T"), null, repo, () -> { });

    assertTrue(dialog.isResizable(), "the edit dialog must be resizable");
    assertTrue(contains(dialog, Tabs.class), "a Visual/Syntax toggle must be present");
    assertTrue(contains(dialog, TextArea.class), "the Syntax (raw) editor must be present");
    assertTrue(contains(dialog, Markdown.class), "the Visual (rendered) markdown must be present");
  }

  @Test
  @DisplayName("a part editor shows a prominent status control; a topic-only editor does not (M)")
  void statusControlOnlyForPart() {
    Issue issue = repo.createIssue("T");
    Part part = issue.addPart();

    TopicEditDialog partEditor = new TopicEditDialog(issue, part, repo, () -> { });
    TopicEditDialog topicEditor = new TopicEditDialog(issue, null, repo, () -> { });

    assertTrue(contains(partEditor, RadioButtonGroup.class),
        "the part editor must surface a prominent editorial-status control");
    assertFalse(contains(topicEditor, RadioButtonGroup.class),
        "the topic-only editor has no status control");
  }

  private static boolean contains(Component root, Class<?> type) {
    return selfAndDescendants(root).anyMatch(type::isInstance);
  }

  private static Stream<Component> selfAndDescendants(Component c) {
    return Stream.concat(Stream.of(c),
        c.getChildren().flatMap(TopicEditDialogTest::selfAndDescendants));
  }
}
