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

package com.svenruppert.publications.persistence;

import com.svenruppert.publications.model.DataRoot;
import com.svenruppert.publications.model.Issue;
import com.svenruppert.publications.model.Language;
import com.svenruppert.publications.model.LanguageVersion;
import com.svenruppert.publications.model.Part;
import com.svenruppert.publications.model.Publication;
import com.svenruppert.publications.model.PublicationPlace;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Domain-facing facade over the {@link DataRoot} — analogous to
 * {@code PersistentUserDirectory}. Loads the graph once, offers read queries and
 * persists after every mutation through {@link PublicationsPersistence}. Views
 * mutate the aggregates directly (their own mutators) and then call
 * {@link #persist()} — or use the {@code create…}/{@code delete…} helpers that
 * persist themselves.
 */
public final class PublicationsRepository {

  private final PublicationsPersistence persistence;
  private final DataRoot root;

  public PublicationsRepository(PublicationsPersistence persistence) {
    this.persistence = Objects.requireNonNull(persistence, "persistence");
    this.root = persistence.load();
  }

  public PublicationsRepository() {
    this(new EclipseStorePublicationsPersistence());
  }

  // ── reads ──────────────────────────────────────────────────────────────

  public List<Issue> issues() {
    return root.issues();
  }

  public List<PublicationPlace> publicationPlaces() {
    return root.publicationPlaces();
  }

  public Optional<Issue> findIssue(UUID id) {
    return root.issues().stream().filter(i -> i.id().equals(id)).findFirst();
  }

  public Optional<Issue> findIssueByOrigin(String origin) {
    if (origin == null) {
      return Optional.empty();
    }
    return root.issues().stream()
        .filter(i -> origin.equals(i.origin()))
        .findFirst();
  }

  public Optional<Part> findPart(UUID id) {
    return root.issues().stream()
        .flatMap(i -> i.parts().stream())
        .filter(p -> p.id().equals(id))
        .findFirst();
  }

  public Optional<LanguageVersion> findVersion(UUID id) {
    return root.issues().stream()
        .flatMap(i -> i.parts().stream())
        .flatMap(p -> p.languageVersions().stream())
        .filter(v -> v.id().equals(id))
        .findFirst();
  }

  public List<Publication> allPublications() {
    return root.issues().stream()
        .flatMap(i -> i.parts().stream())
        .flatMap(p -> p.languageVersions().stream())
        .flatMap(v -> v.publications().stream())
        .toList();
  }

  public Optional<Publication> findPublication(UUID id) {
    return allPublications().stream().filter(v -> v.id().equals(id)).findFirst();
  }

  public Optional<PublicationPlace> findPlace(UUID id) {
    return root.publicationPlaces().stream().filter(o -> o.id().equals(id)).findFirst();
  }

  /**
   * The {@link Part} that owns {@code version} — walks the graph, since a
   * {@link LanguageVersion} keeps no back-reference. Lets a publication resolve
   * the blog post (topic + part) it belongs to.
   */
  public Optional<Part> partOf(LanguageVersion version) {
    if (version == null) {
      return Optional.empty();
    }
    return root.issues().stream()
        .flatMap(i -> i.parts().stream())
        .filter(p -> p.languageVersions().contains(version))
        .findFirst();
  }

  /** Places whose supported languages include {@code language} (language rule). */
  public List<PublicationPlace> placesFor(Language language) {
    return root.publicationPlaces().stream()
        .filter(o -> o.supports(language))
        .toList();
  }

  // ── mutations (with persistence) ─────────────────────────────────────────

  public Issue createIssue(String title) {
    Issue issue = new Issue(title);
    root.addIssue(issue);
    persist();
    return issue;
  }

  public void deleteIssue(Issue issue) {
    root.removeIssue(issue);
    persist();
  }

  public PublicationPlace createPublicationPlace(String name, Set<Language> supportedLanguages) {
    PublicationPlace place = new PublicationPlace(name, supportedLanguages);
    root.addPublicationPlace(place);
    persist();
    return place;
  }

  public void deletePublicationPlace(PublicationPlace place) {
    root.removePublicationPlace(place);
    persist();
  }

  /** Persists the current state after in-place mutation of the aggregates. */
  public void persist() {
    persistence.save(root);
  }

  /** The live data root (for import/reconstruction). */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP",
      justification = "by design — the import/reconstruction path adds aggregates to the live "
          + "root and then calls persist(); a copy would not be persisted")
  public DataRoot dataRoot() {
    return root;
  }
}
