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
 * Ein Publikationsort — in der schlanken Fassung auf Name und die Menge der
 * unterstützten Sprachen verschlankt. Ordnet die harte <em>Sprachregel</em>:
 * eine {@link Veroeffentlichung} darf nur auf einen Ort zielen, dessen
 * unterstützte Sprachen die Sprache der {@link Sprachfassung} einschließen.
 */
public final class Publikationsort {

  private final UUID id;
  private String name;
  private final Set<Sprache> unterstuetzteSprachen;

  public Publikationsort(String name, Set<Sprache> unterstuetzteSprachen) {
    this(UUID.randomUUID(), name, unterstuetzteSprachen);
  }

  public Publikationsort(UUID id, String name, Set<Sprache> unterstuetzteSprachen) {
    this.id = Objects.requireNonNull(id, "id");
    this.name = requireName(name);
    this.unterstuetzteSprachen =
        new LinkedHashSet<>(Objects.requireNonNull(unterstuetzteSprachen, "unterstuetzteSprachen"));
  }

  private static String requireName(String name) {
    Objects.requireNonNull(name, "name");
    String stripped = name.strip();
    if (stripped.isEmpty()) {
      throw new IllegalArgumentException("Publikationsort name must not be blank");
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

  /** Unveränderliche Sicht auf die unterstützten Sprachen. */
  public Set<Sprache> unterstuetzteSprachen() {
    return Set.copyOf(unterstuetzteSprachen);
  }

  /** Die fachliche Zulässigkeitsbedingung: unterstützt der Ort diese Sprache? */
  public boolean unterstuetzt(Sprache sprache) {
    return unterstuetzteSprachen.contains(sprache);
  }

  public void ergaenzeSprache(Sprache sprache) {
    unterstuetzteSprachen.add(Objects.requireNonNull(sprache, "sprache"));
  }

  public void entferneSprache(Sprache sprache) {
    unterstuetzteSprachen.remove(sprache);
  }

  @Override
  public String toString() {
    return name;
  }
}
