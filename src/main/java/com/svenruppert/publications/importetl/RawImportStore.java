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

package com.svenruppert.publications.importetl;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Persists the last raw ClickUp extract on disk so a transform/load can run
 * against locally cached JSON — surviving a restart without re-fetching from the
 * network, and separating the (network) extraction from the (idempotent,
 * repeatable) transform/load.
 *
 * <p>The store keeps two files in its directory: {@code raw.json} (the extract
 * payload) and {@code raw.meta} (the extraction timestamp, ISO-8601). It takes
 * its directory as a constructor argument rather than reading
 * {@code AppStoragePaths} directly, so it carries no dependency on the flow
 * layer and is unit-testable against a {@code @TempDir}. The caller supplies the
 * extraction timestamp explicitly (no embedded clock), mirroring the domain's
 * clock-free status history.
 */
public final class RawImportStore {

  private static final String RAW_FILE = "raw.json";
  private static final String META_FILE = "raw.meta";

  private final Path dir;

  public RawImportStore(Path dir) {
    this.dir = Objects.requireNonNull(dir, "dir");
  }

  /**
   * Writes {@code json} and the {@code when} extraction timestamp, replacing any
   * previous extract. Creates the directory if needed.
   */
  public void save(String json, Instant when) {
    Objects.requireNonNull(json, "json");
    Objects.requireNonNull(when, "when");
    try {
      Files.createDirectories(dir);
      Files.writeString(dir.resolve(RAW_FILE), json, StandardCharsets.UTF_8);
      Files.writeString(dir.resolve(META_FILE), when.toString(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException("Could not persist the raw ClickUp extract", e);
    }
  }

  /** The last persisted raw JSON, or empty when nothing has been extracted yet. */
  public Optional<String> loadRaw() {
    Path raw = dir.resolve(RAW_FILE);
    if (!Files.isRegularFile(raw)) {
      return Optional.empty();
    }
    try {
      return Optional.of(Files.readString(raw, StandardCharsets.UTF_8));
    } catch (IOException e) {
      throw new UncheckedIOException("Could not read the raw ClickUp extract", e);
    }
  }

  /** The timestamp of the last extraction, or empty when nothing has been extracted yet. */
  public Optional<Instant> lastExtraction() {
    Path meta = dir.resolve(META_FILE);
    if (!Files.isRegularFile(meta)) {
      return Optional.empty();
    }
    try {
      return Optional.of(Instant.parse(Files.readString(meta, StandardCharsets.UTF_8).strip()));
    } catch (IOException e) {
      throw new UncheckedIOException("Could not read the extract timestamp", e);
    }
  }
}
