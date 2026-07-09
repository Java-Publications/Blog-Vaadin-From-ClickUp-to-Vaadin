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

package com.svenruppert.publications.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * A language-neutral content unit below an {@link Issue}, determined by its issue
 * and position. It carries exactly one editorial {@link EditorialState} (through
 * the encapsulated {@link StatusHistory}) and holds its {@link LanguageVersion}s
 * — at most one per {@link Language}.
 */
public final class Part {

  private final UUID id;
  private Issue issue;
  private int position;
  private final StatusHistory<EditorialState> editorialWork;
  private final List<LanguageVersion> languageVersions = new ArrayList<>();

  public Part(int position) {
    this(UUID.randomUUID(), position);
  }

  public Part(UUID id, int position) {
    this.id = Objects.requireNonNull(id, "id");
    this.position = position;
    this.editorialWork = new StatusHistory<>(EditorialState.BACKLOG);
  }

  /** Sets the back-reference within the aggregate; only called by {@link Issue}. */
  void setIssue(Issue issue) {
    this.issue = issue;
  }

  public UUID id() {
    return id;
  }

  public Issue issue() {
    return issue;
  }

  public int position() {
    return position;
  }

  void setPosition(int position) {
    this.position = position;
  }

  // ── editorial state ──────────────────────────────────────────────────────

  public StatusHistory<EditorialState> editorialWork() {
    return editorialWork;
  }

  public EditorialState editorialState() {
    return editorialWork.current();
  }

  public void changeState(EditorialState to, String actor) {
    editorialWork.record(to, actor);
  }

  // ── language versions ────────────────────────────────────────────────────

  /** Creates a language version; at most one per language. */
  public LanguageVersion addLanguageVersion(Language language) {
    Objects.requireNonNull(language, "language");
    if (versionFor(language).isPresent()) {
      throw new IllegalArgumentException("Language version for " + language + " already exists");
    }
    LanguageVersion version = new LanguageVersion(language);
    languageVersions.add(version);
    return version;
  }

  /** Appends an already-constructed version (import/reconstruction). */
  public LanguageVersion adopt(LanguageVersion version) {
    Objects.requireNonNull(version, "version");
    if (versionFor(version.language()).isPresent()) {
      throw new IllegalArgumentException("Language version for " + version.language() + " already exists");
    }
    languageVersions.add(version);
    return version;
  }

  public Optional<LanguageVersion> versionFor(Language language) {
    return languageVersions.stream().filter(v -> v.language() == language).findFirst();
  }

  public List<LanguageVersion> languageVersions() {
    return List.copyOf(languageVersions);
  }
}
