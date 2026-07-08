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

import com.svenruppert.publications.model.Datenwurzel;
import com.svenruppert.publications.model.Issue;
import com.svenruppert.publications.model.Publikationsort;
import com.svenruppert.publications.model.Sprache;
import com.svenruppert.publications.model.Sprachfassung;
import com.svenruppert.publications.model.Teil;
import com.svenruppert.publications.model.Veroeffentlichung;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Fachschnittstelle über die {@link Datenwurzel} — analog zu
 * {@code PersistentUserDirectory}. Lädt den Graph einmal, bietet Lese-Abfragen
 * und persistiert nach jeder Mutation über {@link PublicationsPersistence}.
 * Views mutieren die Aggregate direkt (deren eigene Mutatoren) und rufen danach
 * {@link #persist()} — oder nutzen die {@code neu…}/{@code loesche…}-Helfer,
 * die selbst persistieren.
 */
public final class PublicationsRepository {

  private final PublicationsPersistence persistence;
  private final Datenwurzel wurzel;

  public PublicationsRepository(PublicationsPersistence persistence) {
    this.persistence = Objects.requireNonNull(persistence, "persistence");
    this.wurzel = persistence.load();
  }

  public PublicationsRepository() {
    this(new EclipseStorePublicationsPersistence());
  }

  // ── Lesen ────────────────────────────────────────────────────────────────

  public List<Issue> issues() {
    return wurzel.issues();
  }

  public List<Publikationsort> publikationsorte() {
    return wurzel.publikationsorte();
  }

  public Optional<Issue> findIssue(UUID id) {
    return wurzel.issues().stream().filter(i -> i.id().equals(id)).findFirst();
  }

  public Optional<Issue> findIssueByHerkunft(String herkunft) {
    if (herkunft == null) {
      return Optional.empty();
    }
    return wurzel.issues().stream()
        .filter(i -> herkunft.equals(i.herkunft()))
        .findFirst();
  }

  public Optional<Teil> findTeil(UUID id) {
    return wurzel.issues().stream()
        .flatMap(i -> i.teile().stream())
        .filter(t -> t.id().equals(id))
        .findFirst();
  }

  public Optional<Sprachfassung> findFassung(UUID id) {
    return wurzel.issues().stream()
        .flatMap(i -> i.teile().stream())
        .flatMap(t -> t.sprachfassungen().stream())
        .filter(f -> f.id().equals(id))
        .findFirst();
  }

  public List<Veroeffentlichung> alleVeroeffentlichungen() {
    return wurzel.issues().stream()
        .flatMap(i -> i.teile().stream())
        .flatMap(t -> t.sprachfassungen().stream())
        .flatMap(f -> f.veroeffentlichungen().stream())
        .toList();
  }

  public Optional<Veroeffentlichung> findVeroeffentlichung(UUID id) {
    return alleVeroeffentlichungen().stream().filter(v -> v.id().equals(id)).findFirst();
  }

  public Optional<Publikationsort> findOrt(UUID id) {
    return wurzel.publikationsorte().stream().filter(o -> o.id().equals(id)).findFirst();
  }

  /** Orte, deren unterstützte Sprachen {@code sprache} einschließen (Sprachregel). */
  public List<Publikationsort> orteFuer(Sprache sprache) {
    return wurzel.publikationsorte().stream()
        .filter(o -> o.unterstuetzt(sprache))
        .toList();
  }

  // ── Mutieren (mit Persistierung) ──────────────────────────────────────────

  public Issue neuesIssue(String titel) {
    Issue issue = new Issue(titel);
    wurzel.addIssue(issue);
    persist();
    return issue;
  }

  public void loescheIssue(Issue issue) {
    wurzel.removeIssue(issue);
    persist();
  }

  public Publikationsort neuerPublikationsort(String name, Set<Sprache> unterstuetzteSprachen) {
    Publikationsort ort = new Publikationsort(name, unterstuetzteSprachen);
    wurzel.addPublikationsort(ort);
    persist();
    return ort;
  }

  public void loeschePublikationsort(Publikationsort ort) {
    wurzel.removePublikationsort(ort);
    persist();
  }

  /** Persistiert den aktuellen Stand nach In-place-Mutation der Aggregate. */
  public void persist() {
    persistence.save(wurzel);
  }

  /** Die lebende Datenwurzel (für Import/Rekonstruktion). */
  public Datenwurzel datenwurzel() {
    return wurzel;
  }
}
