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
import java.util.UUID;

/**
 * The actual manuscript per language. The planned character count lives here,
 * because it differs per language. A language version has no status dimension of
 * its own; its progress is reflected indirectly in the {@link EditorialState} of
 * its {@link Part} and in the status of its {@link Publication}s.
 */
public final class LanguageVersion {

  private final UUID id;
  private final Language language;
  private String manuscript;
  private int plannedCharacters;
  private final List<Publication> publications = new ArrayList<>();

  public LanguageVersion(Language language) {
    this(UUID.randomUUID(), language);
  }

  public LanguageVersion(UUID id, Language language) {
    this.id = Objects.requireNonNull(id, "id");
    this.language = Objects.requireNonNull(language, "language");
  }

  public UUID id() {
    return id;
  }

  public Language language() {
    return language;
  }

  /** Reference/URL to the document. */
  public String manuscript() {
    return manuscript;
  }

  public void setManuscript(String manuscript) {
    this.manuscript = manuscript;
  }

  public int plannedCharacters() {
    return plannedCharacters;
  }

  public void setPlannedCharacters(int plannedCharacters) {
    if (plannedCharacters < 0) {
      throw new IllegalArgumentException("plannedCharacters must be >= 0, was " + plannedCharacters);
    }
    this.plannedCharacters = plannedCharacters;
  }

  /** Checks the language rule for a candidate place without mutating. */
  public boolean canBePublishedAt(PublicationPlace place) {
    return place != null && place.supports(language);
  }

  /**
   * Plans a new publication of this version at {@code place} and appends it.
   * Throws {@link IllegalArgumentException} if the language rule is violated —
   * callers check first with {@link #canBePublishedAt}.
   */
  public Publication planPublication(PublicationPlace place) {
    Publication publication = new Publication(this, place);
    publications.add(publication);
    return publication;
  }

  /** Appends an already-constructed publication (import/reconstruction). */
  public Publication adopt(Publication publication) {
    Objects.requireNonNull(publication, "publication");
    if (publication.version() != this) {
      throw new IllegalArgumentException("Publication belongs to a different version");
    }
    publications.add(publication);
    return publication;
  }

  public List<Publication> publications() {
    return List.copyOf(publications);
  }
}
