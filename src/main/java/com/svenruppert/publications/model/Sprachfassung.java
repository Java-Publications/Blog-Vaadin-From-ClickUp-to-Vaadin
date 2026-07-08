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
 * Das eigentliche Manuskript je Sprache. An ihr ist die geplante Zeichenzahl
 * beheimatet, da sie sich je Sprache unterscheidet. Eine eigene Statusdimension
 * besitzt die Sprachfassung nicht; ihr Fortschritt spiegelt sich mittelbar im
 * {@link Arbeitszustand} ihres {@link Teil}s und im Status ihrer
 * {@link Veroeffentlichung}en.
 */
public final class Sprachfassung {

  private final UUID id;
  private final Sprache sprache;
  private String manuskript;
  private int geplanteZeichen;
  private final List<Veroeffentlichung> veroeffentlichungen = new ArrayList<>();

  public Sprachfassung(Sprache sprache) {
    this(UUID.randomUUID(), sprache);
  }

  public Sprachfassung(UUID id, Sprache sprache) {
    this.id = Objects.requireNonNull(id, "id");
    this.sprache = Objects.requireNonNull(sprache, "sprache");
  }

  public UUID id() {
    return id;
  }

  public Sprache sprache() {
    return sprache;
  }

  /** Verweis/URL auf das Dokument. */
  public String manuskript() {
    return manuskript;
  }

  public void setManuskript(String manuskript) {
    this.manuskript = manuskript;
  }

  public int geplanteZeichen() {
    return geplanteZeichen;
  }

  public void setGeplanteZeichen(int geplanteZeichen) {
    if (geplanteZeichen < 0) {
      throw new IllegalArgumentException("geplanteZeichen must be >= 0, was " + geplanteZeichen);
    }
    this.geplanteZeichen = geplanteZeichen;
  }

  /** Prüft die Sprachregel für einen möglichen Ort, ohne zu verändern. */
  public boolean kannVeroeffentlichtWerdenAn(Publikationsort ort) {
    return ort != null && ort.unterstuetzt(sprache);
  }

  /**
   * Plant eine neue Veröffentlichung dieser Fassung an {@code ort} und hängt sie
   * an. Wirft {@link IllegalArgumentException}, wenn die Sprachregel verletzt ist
   * — Aufrufer prüfen vorher mit {@link #kannVeroeffentlichtWerdenAn}.
   */
  public Veroeffentlichung planeVeroeffentlichung(Publikationsort ort) {
    Veroeffentlichung v = new Veroeffentlichung(this, ort);
    veroeffentlichungen.add(v);
    return v;
  }

  /** Hängt eine bereits konstruierte Veröffentlichung an (Import/Rekonstruktion). */
  public Veroeffentlichung uebernimm(Veroeffentlichung veroeffentlichung) {
    Objects.requireNonNull(veroeffentlichung, "veroeffentlichung");
    if (veroeffentlichung.fassung() != this) {
      throw new IllegalArgumentException("Veroeffentlichung gehört zu einer anderen Fassung");
    }
    veroeffentlichungen.add(veroeffentlichung);
    return veroeffentlichung;
  }

  public List<Veroeffentlichung> veroeffentlichungen() {
    return List.copyOf(veroeffentlichungen);
  }
}
