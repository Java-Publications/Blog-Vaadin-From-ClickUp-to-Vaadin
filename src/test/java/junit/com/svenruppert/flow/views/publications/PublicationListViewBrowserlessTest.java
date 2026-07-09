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
import com.svenruppert.flow.views.publications.PublicationListView;
import com.svenruppert.jsentinel.authorization.api.SubjectStores;
import com.svenruppert.publications.model.Issue;
import com.svenruppert.publications.model.Language;
import com.svenruppert.publications.model.LanguageVersion;
import com.svenruppert.publications.model.Part;
import com.svenruppert.publications.model.PublicationPlace;
import com.svenruppert.publications.persistence.InMemoryPublicationsPersistence;
import com.svenruppert.publications.persistence.PublicationsProvider;
import com.svenruppert.publications.persistence.PublicationsRepository;
import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import junit.com.svenruppert.flow.TestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("PublicationListView — overview grid")
class PublicationListViewBrowserlessTest extends BrowserlessTest {

  @BeforeEach
  void setUp() {
    TestSupport.seedAdminAndResetBootstrap();
    UI ui = UI.getCurrent();
    if (ui != null) {
      ui.setLocale(Locale.ENGLISH);
    }
    PublicationsRepository repo =
        new PublicationsRepository(new InMemoryPublicationsPersistence());
    PublicationPlace place = repo.createPublicationPlace("svenruppert.com", Set.of(Language.GERMAN));
    Issue issue = repo.createIssue("Blog – Navigation – Coupled navigation");
    Part part = issue.addPart();
    LanguageVersion de = part.addLanguageVersion(Language.GERMAN);
    de.planPublication(place);
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
  @DisplayName("NAV is 'veroeffentlichungen' and the grid lists the publication")
  void listsPublications() {
    assertEquals("veroeffentlichungen", PublicationListView.NAV);
    UI.getCurrent().navigate(PublicationListView.class);
    assertEquals("Publications", $view(H1.class).first().getText());
    Grid<?> grid = $view(Grid.class).first();
    assertEquals(6, grid.getColumns().size());
    assertEquals(1, grid.getListDataView().getItemCount());
  }
}
