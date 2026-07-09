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
import com.svenruppert.flow.views.publications.PublicationView;
import com.svenruppert.jsentinel.authorization.api.SubjectStores;
import com.svenruppert.publications.model.Issue;
import com.svenruppert.publications.model.Language;
import com.svenruppert.publications.model.LanguageVersion;
import com.svenruppert.publications.model.Part;
import com.svenruppert.publications.model.Publication;
import com.svenruppert.publications.model.PublicationPlace;
import com.svenruppert.publications.persistence.InMemoryPublicationsPersistence;
import com.svenruppert.publications.persistence.PublicationsProvider;
import com.svenruppert.publications.persistence.PublicationsRepository;
import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.select.Select;
import junit.com.svenruppert.flow.TestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("PublicationView (V4) — two orthogonal columns")
class PublicationViewBrowserlessTest extends BrowserlessTest {

  private UUID vId;

  @BeforeEach
  void setUp() {
    TestSupport.seedAdminAndResetBootstrap();
    UI ui = UI.getCurrent();
    if (ui != null) {
      ui.setLocale(Locale.ENGLISH);
    }
    PublicationsRepository repo =
        new PublicationsRepository(new InMemoryPublicationsPersistence());
    PublicationPlace place =
        repo.createPublicationPlace("svenruppert.com", Set.of(Language.GERMAN));
    Issue issue = repo.createIssue("Blog – Navigation – Coupled navigation");
    Part part = issue.addPart();
    LanguageVersion de = part.addLanguageVersion(Language.GERMAN);
    Publication v = de.planPublication(place);
    repo.persist();
    vId = v.id();
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
  @DisplayName("NAV constant is 'veroeffentlichung'")
  void navConstant() {
    assertEquals("veroeffentlichung", PublicationView.NAV);
  }

  @Test
  @DisplayName("renders both lifecycle columns with their initial states")
  void rendersTwoColumns() {
    UI.getCurrent().navigate(PublicationView.class, vId.toString());

    assertEquals("Publication", $view(H1.class).first().getText());

    List<String> spanTexts = $view(Span.class).all().stream()
        .map(Span::getText).collect(Collectors.toList());
    assertTrue(spanTexts.contains("ACQUISITION"), "left column is Acquisition");
    assertTrue(spanTexts.contains("PRODUCTION"), "right column is Production");
    assertTrue(spanTexts.contains("REQUESTED"), "acquisition starts at REQUESTED");
    assertTrue(spanTexts.contains("PLANNED"), "production starts at PLANNED");

    assertEquals(2, $view(Select.class).all().size(),
        "one advance-Select per dimension");
  }

  @Test
  @DisplayName("unknown id → empty state")
  void notFound() {
    UI.getCurrent().navigate(PublicationView.class, UUID.randomUUID().toString());
    assertEquals("Publication not found", $view(H3.class).first().getText());
  }
}
