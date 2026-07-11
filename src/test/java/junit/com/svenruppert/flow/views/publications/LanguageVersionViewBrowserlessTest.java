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
import com.svenruppert.flow.views.publications.LanguageVersionView;
import com.svenruppert.jsentinel.authorization.api.SubjectStores;
import com.svenruppert.publications.model.Issue;
import com.svenruppert.publications.model.Language;
import com.svenruppert.publications.model.Part;
import com.svenruppert.publications.persistence.InMemoryPublicationsPersistence;
import com.svenruppert.publications.persistence.PublicationsProvider;
import com.svenruppert.publications.persistence.PublicationsRepository;
import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
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
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("LanguageVersionView (V2) — parametrised route + editor")
class LanguageVersionViewBrowserlessTest extends BrowserlessTest {

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
    repo.createPublicationPlace("svenruppert.com", java.util.Set.of(Language.GERMAN));
    Issue issue = repo.createIssue("Blog – Navigation – Coupled navigation");
    Part part = issue.addPart();
    part.addLanguageVersion(Language.GERMAN);
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
  @DisplayName("NAV constant is 'teil'")
  void navConstant() {
    assertEquals("teil", LanguageVersionView.NAV);
  }

  @Test
  @DisplayName("renders the editor: header, next-step guidance/empty-state, plan + back buttons")
  void rendersEditorForValidPart() {
    UI.getCurrent().navigate(LanguageVersionView.class, partId.toString());
    H1 heading = $view(H1.class).first();
    assertEquals("Language versions", heading.getText());

    List<String> buttons = $view(Button.class).all().stream()
        .map(Button::getText).collect(Collectors.toList());
    assertTrue(buttons.contains("Plan publication"),
        "editor must offer the language-rule-guarded plan action");
    assertTrue(buttons.contains("Back to topic workspace"),
        "editor must offer a back button to the topic workspace (S)");
    // With no publications yet, an empty-state guides to the next step (Q).
    assertTrue($view(H3.class).all().stream()
            .anyMatch(h -> "Not published anywhere yet".equals(h.getText())),
        "the next-step empty-state must guide the user to plan a publication");
  }

  @Test
  @DisplayName("unknown part id → empty state")
  void notFoundForUnknownPart() {
    UI.getCurrent().navigate(LanguageVersionView.class, UUID.randomUUID().toString());
    H3 empty = $view(H3.class).first();
    assertEquals("Part not found", empty.getText());
  }
}
