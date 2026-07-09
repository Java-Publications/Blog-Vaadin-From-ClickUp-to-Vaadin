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
import com.svenruppert.publications.model.Issue;
import com.svenruppert.publications.model.Tag;
import com.svenruppert.publications.persistence.InMemoryPublicationsPersistence;
import com.svenruppert.publications.persistence.PublicationsProvider;
import com.svenruppert.publications.persistence.PublicationsRepository;
import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import junit.com.svenruppert.flow.TestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Locale;
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
    assertEquals("themen", TopicsView.NAV);
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
  @DisplayName("toolbar has a '+ Topic' button")
  void createTopicButtonPresent() {
    UI.getCurrent().navigate(TopicsView.class);
    assertTrue($view(Button.class).all().stream()
            .map(Button::getText)
            .collect(Collectors.toList())
            .contains("+ Topic"),
        "the header must offer a '+ Topic' action");
  }
}
