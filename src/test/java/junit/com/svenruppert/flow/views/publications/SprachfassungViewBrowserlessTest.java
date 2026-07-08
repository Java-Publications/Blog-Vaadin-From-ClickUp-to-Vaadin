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
import com.svenruppert.flow.views.publications.SprachfassungView;
import com.svenruppert.jsentinel.authorization.api.SubjectStores;
import com.svenruppert.publications.model.Issue;
import com.svenruppert.publications.model.Sprache;
import com.svenruppert.publications.model.Teil;
import com.svenruppert.publications.persistence.InMemoryPublicationsPersistence;
import com.svenruppert.publications.persistence.PublicationsProvider;
import com.svenruppert.publications.persistence.PublicationsRepository;
import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
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
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("SprachfassungView (V2) — parametrised route + editor")
class SprachfassungViewBrowserlessTest extends BrowserlessTest {

  private UUID teilId;

  @BeforeEach
  void setUp() {
    TestSupport.seedAdminAndResetBootstrap();
    UI ui = UI.getCurrent();
    if (ui != null) {
      ui.setLocale(Locale.ENGLISH);
    }
    PublicationsRepository repo =
        new PublicationsRepository(new InMemoryPublicationsPersistence());
    repo.neuerPublikationsort("svenruppert.com", java.util.Set.of(Sprache.DEUTSCH));
    Issue issue = repo.neuesIssue("Blog – Navigation – Koppelnavigation");
    Teil teil = issue.addTeil();
    teil.addSprachfassung(Sprache.DEUTSCH);
    repo.persist();
    teilId = teil.id();
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
  @DisplayName("NAV constant is 'teil'")
  void navConstant() {
    assertEquals("teil", SprachfassungView.NAV);
  }

  @Test
  @DisplayName("renders the editor for a valid part id — header, publications grid, plan button")
  void rendersEditorForValidTeil() {
    UI.getCurrent().navigate(SprachfassungView.class, teilId.toString());
    H1 heading = $view(H1.class).first();
    assertEquals("Language versions", heading.getText());
    // one publications grid for the DE version (empty)
    Grid<?> grid = $view(Grid.class).first();
    assertEquals(0, grid.getListDataView().getItemCount());
    assertTrue($view(Button.class).all().stream()
            .map(Button::getText).collect(Collectors.toList())
            .contains("Plan publication"),
        "editor must offer the Sprachregel-guarded plan action");
  }

  @Test
  @DisplayName("unknown part id → empty state")
  void notFoundForUnknownTeil() {
    UI.getCurrent().navigate(SprachfassungView.class, UUID.randomUUID().toString());
    H3 empty = $view(H3.class).first();
    assertEquals("Part not found", empty.getText());
  }
}
