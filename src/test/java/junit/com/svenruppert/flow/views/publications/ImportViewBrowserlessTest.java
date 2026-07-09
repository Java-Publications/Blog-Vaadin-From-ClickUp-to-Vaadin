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
import com.svenruppert.flow.security.storage.AppStoragePaths;
import com.svenruppert.flow.views.publications.ImportView;
import com.svenruppert.jsentinel.authorization.api.SubjectStores;
import com.svenruppert.publications.importetl.RawImportStore;
import com.svenruppert.publications.persistence.InMemoryPublicationsPersistence;
import com.svenruppert.publications.persistence.PublicationsProvider;
import com.svenruppert.publications.persistence.PublicationsRepository;
import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import junit.com.svenruppert.flow.TestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ImportView (V7) — ETL console")
class ImportViewBrowserlessTest extends BrowserlessTest {

  @BeforeEach
  void setUp() throws IOException {
    TestSupport.seedAdminAndResetBootstrap();
    UI ui = UI.getCurrent();
    if (ui != null) {
      ui.setLocale(Locale.ENGLISH);
    }
    PublicationsProvider.setRepository(
        new PublicationsRepository(new InMemoryPublicationsPersistence()));
    SubjectStores.subjectStore().setCurrentSubject(
        new AppUser(1L, "Alice", EnumSet.of(AuthorizationRole.ADMIN, AuthorizationRole.USER)),
        AppUser.class);
    cleanImportDir();
  }

  @AfterEach
  void tearDown() throws IOException {
    PublicationsProvider.reset();
    SubjectStores.subjectStore().deleteCurrentSubject(AppUser.class);
    cleanImportDir();
  }

  @Test
  @DisplayName("NAV is 'import'; console renders the three ETL steps")
  void consoleRendersThreeSteps() {
    assertEquals("import", ImportView.NAV);
    UI.getCurrent().navigate(ImportView.class);
    assertEquals("Import console", $view(H1.class).first().getText());
    List<String> buttons = $view(Button.class).all().stream()
        .map(Button::getText).collect(Collectors.toList());
    assertTrue(buttons.contains("① Extract"));
    assertTrue(buttons.contains("② Transform & load"));
    assertTrue(buttons.contains("③ Repeat run"));
  }

  @Test
  @DisplayName("with no cached extract, the console reports 'No local extract yet.'")
  void noCachedExtractShowsNever() {
    UI.getCurrent().navigate(ImportView.class);
    assertTrue(spanTexts().contains("No local extract yet."),
        "a clean import dir must render the 'never extracted' state");
  }

  @Test
  @DisplayName("a raw extract cached on disk is adopted on open — the import survives a restart")
  void adoptsCachedRawExtractOnOpen() {
    // Simulate a prior extract persisted before this "restart".
    new RawImportStore(AppStoragePaths.importDir())
        .save("{\"tasks\":[]}", Instant.parse("2026-07-09T08:30:00Z"));

    UI.getCurrent().navigate(ImportView.class);

    // The last-extraction line renders (not the 'never' state) ...
    assertTrue(spanTexts().stream().anyMatch(t -> t.startsWith("Last extraction:")),
        "the timestamp of the cached extract must render");
    // ... and the raw payload (12 bytes) is adopted, ready for ②.
    assertTrue($view(com.vaadin.flow.component.html.Div.class).all().stream()
            .anyMatch(d -> d.getText().contains("Loaded 12 bytes")),
        "the cached raw extract must be adopted on open");
  }

  private List<String> spanTexts() {
    return $view(Span.class).all().stream().map(Span::getText).collect(Collectors.toList());
  }

  private static void cleanImportDir() throws IOException {
    Path dir = AppStoragePaths.importDir();
    if (!Files.exists(dir)) {
      return;
    }
    try (Stream<Path> walk = Files.walk(dir)) {
      walk.sorted(Comparator.reverseOrder()).forEach(p -> {
        try {
          Files.deleteIfExists(p);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      });
    }
  }
}
