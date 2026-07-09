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

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * A publication place — in the lean model reduced to a name and the set of
 * supported languages. It carries the hard <em>language rule</em>: a
 * {@link Publication} may only target a place whose supported languages include
 * the language of the {@link LanguageVersion}.
 */
public final class PublicationPlace {

  private final UUID id;
  private String name;
  private final Set<Language> supportedLanguages;

  public PublicationPlace(String name, Set<Language> supportedLanguages) {
    this(UUID.randomUUID(), name, supportedLanguages);
  }

  public PublicationPlace(UUID id, String name, Set<Language> supportedLanguages) {
    this.id = Objects.requireNonNull(id, "id");
    this.name = requireName(name);
    this.supportedLanguages =
        new LinkedHashSet<>(Objects.requireNonNull(supportedLanguages, "supportedLanguages"));
  }

  private static String requireName(String name) {
    Objects.requireNonNull(name, "name");
    String stripped = name.strip();
    if (stripped.isEmpty()) {
      throw new IllegalArgumentException("PublicationPlace name must not be blank");
    }
    return stripped;
  }

  public UUID id() {
    return id;
  }

  public String name() {
    return name;
  }

  public void setName(String name) {
    this.name = requireName(name);
  }

  /** Immutable view of the supported languages. */
  public Set<Language> supportedLanguages() {
    return Set.copyOf(supportedLanguages);
  }

  /** The language rule's admissibility check: does this place support the language? */
  public boolean supports(Language language) {
    return supportedLanguages.contains(language);
  }

  public void addLanguage(Language language) {
    supportedLanguages.add(Objects.requireNonNull(language, "language"));
  }

  public void removeLanguage(Language language) {
    supportedLanguages.remove(language);
  }

  @Override
  public String toString() {
    return name;
  }
}
