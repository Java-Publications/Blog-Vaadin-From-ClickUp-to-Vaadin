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

package junit.com.svenruppert.publications.importetl;

import com.svenruppert.publications.importetl.RawImportStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("RawImportStore — round-trips the last raw extract on disk")
class RawImportStoreTest {

  @Test
  @DisplayName("save then load returns the exact JSON and timestamp")
  void savePersistsRawAndTimestamp(@TempDir Path dir) {
    RawImportStore store = new RawImportStore(dir);
    String json = "{\"tasks\":[{\"id\":\"abc\"}]}";
    Instant when = Instant.parse("2026-07-09T10:15:30.00Z");

    store.save(json, when);

    assertEquals(Optional.of(json), store.loadRaw());
    assertEquals(Optional.of(when), store.lastExtraction());
  }

  @Test
  @DisplayName("an untouched store reports nothing extracted yet")
  void emptyStoreReportsAbsent(@TempDir Path dir) {
    RawImportStore store = new RawImportStore(dir);
    assertTrue(store.loadRaw().isEmpty(), "no raw before the first save");
    assertTrue(store.lastExtraction().isEmpty(), "no timestamp before the first save");
  }

  @Test
  @DisplayName("a second save overwrites the first — only the latest extract survives")
  void secondSaveOverwrites(@TempDir Path dir) {
    RawImportStore store = new RawImportStore(dir);
    store.save("{\"tasks\":[]}", Instant.parse("2026-01-01T00:00:00Z"));
    store.save("{\"tasks\":[{\"id\":\"x\"}]}", Instant.parse("2026-07-09T12:00:00Z"));

    assertEquals(Optional.of("{\"tasks\":[{\"id\":\"x\"}]}"), store.loadRaw());
    assertEquals(Optional.of(Instant.parse("2026-07-09T12:00:00Z")), store.lastExtraction());
  }

  @Test
  @DisplayName("a fresh store over the same directory reads what a prior store wrote (survives a restart)")
  void anotherStoreOverSameDirSeesTheData(@TempDir Path dir) {
    new RawImportStore(dir).save("{\"tasks\":[{\"id\":\"persisted\"}]}",
        Instant.parse("2026-07-09T09:00:00Z"));

    // A new instance stands in for a process restart pointing at the same dir.
    RawImportStore afterRestart = new RawImportStore(dir);
    assertFalse(afterRestart.loadRaw().isEmpty(), "the cached extract must survive a restart");
    assertTrue(afterRestart.loadRaw().orElseThrow().contains("persisted"));
  }
}
