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
 * Sprachneutrale Inhaltseinheit unterhalb eines {@link Issue}s, bestimmt durch
 * Issue und Reihenfolge. Trägt genau einen redaktionellen {@link Arbeitszustand}
 * (über den gekapselten {@link Statusverlauf}) und hält seine
 * {@link Sprachfassung}en — höchstens eine je {@link Sprache}.
 */
public final class Teil {

  private final UUID id;
  private Issue issue;
  private int reihenfolge;
  private final Statusverlauf<Arbeitszustand> arbeit;
  private final List<Sprachfassung> sprachfassungen = new ArrayList<>();

  public Teil(int reihenfolge) {
    this(UUID.randomUUID(), reihenfolge);
  }

  public Teil(UUID id, int reihenfolge) {
    this.id = Objects.requireNonNull(id, "id");
    this.reihenfolge = reihenfolge;
    this.arbeit = new Statusverlauf<>(Arbeitszustand.BACKLOG);
  }

  /** Rückreferenz-Setzen im Aggregat; nur durch {@link Issue} aufgerufen. */
  void setIssue(Issue issue) {
    this.issue = issue;
  }

  public UUID id() {
    return id;
  }

  public Issue issue() {
    return issue;
  }

  public int reihenfolge() {
    return reihenfolge;
  }

  void setReihenfolge(int reihenfolge) {
    this.reihenfolge = reihenfolge;
  }

  // ── redaktioneller Arbeitszustand ────────────────────────────────────────

  public Statusverlauf<Arbeitszustand> arbeit() {
    return arbeit;
  }

  public Arbeitszustand arbeitszustand() {
    return arbeit.aktuell();
  }

  public void wechsleZustand(Arbeitszustand neu, String akteur) {
    arbeit.protokolliere(neu, akteur);
  }

  // ── Sprachfassungen ──────────────────────────────────────────────────────

  /** Legt eine Sprachfassung an; höchstens eine je Sprache. */
  public Sprachfassung addSprachfassung(Sprache sprache) {
    Objects.requireNonNull(sprache, "sprache");
    if (fassung(sprache).isPresent()) {
      throw new IllegalArgumentException("Sprachfassung für " + sprache + " existiert bereits");
    }
    Sprachfassung fassung = new Sprachfassung(sprache);
    sprachfassungen.add(fassung);
    return fassung;
  }

  /** Hängt eine bereits konstruierte Fassung an (Import/Rekonstruktion). */
  public Sprachfassung uebernimm(Sprachfassung fassung) {
    Objects.requireNonNull(fassung, "fassung");
    if (fassung(fassung.sprache()).isPresent()) {
      throw new IllegalArgumentException("Sprachfassung für " + fassung.sprache() + " existiert bereits");
    }
    sprachfassungen.add(fassung);
    return fassung;
  }

  public Optional<Sprachfassung> fassung(Sprache sprache) {
    return sprachfassungen.stream().filter(f -> f.sprache() == sprache).findFirst();
  }

  public List<Sprachfassung> sprachfassungen() {
    return List.copyOf(sprachfassungen);
  }
}
