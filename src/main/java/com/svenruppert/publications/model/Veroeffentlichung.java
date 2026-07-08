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

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Verbindet eine {@link Sprachfassung} mit einem {@link Publikationsort} und
 * trägt <em>zwei</em> orthogonale Lebenszyklen zugleich: die Herstellung
 * ({@link Veroeffentlichungsstatus}, Ausgangszustand {@code PLANNED}) und die
 * reduzierte Akquise ({@link Vertriebsstatus}, Ausgangszustand
 * {@code REQUESTED}). Der redaktionelle Arbeitszustand verbleibt am {@link Teil}.
 *
 * <p>Die Sprache einer Veröffentlichung ist stets die ihrer Fassung; die
 * <em>Sprachregel</em> (der Ort muss diese Sprache unterstützen) wird im
 * Konstruktor als Invariante durchgesetzt.
 */
public final class Veroeffentlichung {

  private final UUID id;
  private final Sprachfassung fassung;
  private Publikationsort ort;

  private final Statusverlauf<Veroeffentlichungsstatus> herstellung;
  private LocalDate datum;
  private String verweis;

  private final Statusverlauf<Vertriebsstatus> akquise;
  private String auftraggeber;

  public Veroeffentlichung(Sprachfassung fassung, Publikationsort ort) {
    this(UUID.randomUUID(), fassung, ort);
  }

  public Veroeffentlichung(UUID id, Sprachfassung fassung, Publikationsort ort) {
    this.id = Objects.requireNonNull(id, "id");
    this.fassung = Objects.requireNonNull(fassung, "fassung");
    this.ort = pruefeSprachregel(fassung, Objects.requireNonNull(ort, "ort"));
    this.herstellung = new Statusverlauf<>(Veroeffentlichungsstatus.PLANNED);
    this.akquise = new Statusverlauf<>(Vertriebsstatus.REQUESTED);
  }

  private static Publikationsort pruefeSprachregel(Sprachfassung fassung, Publikationsort ort) {
    if (!ort.unterstuetzt(fassung.sprache())) {
      throw new IllegalArgumentException(
          "Sprachregel verletzt: Ort '" + ort.name() + "' unterstützt "
              + fassung.sprache() + " nicht");
    }
    return ort;
  }

  public UUID id() {
    return id;
  }

  public Sprachfassung fassung() {
    return fassung;
  }

  /** Die Sprache der Veröffentlichung ist stets die ihrer Fassung. */
  public Sprache sprache() {
    return fassung.sprache();
  }

  public Publikationsort ort() {
    return ort;
  }

  /** Wechsel des Orts unter erneuter Prüfung der Sprachregel. */
  public void setOrt(Publikationsort neuerOrt) {
    this.ort = pruefeSprachregel(fassung, Objects.requireNonNull(neuerOrt, "neuerOrt"));
  }

  // ── Dimension Herstellung ───────────────────────────────────────────────

  public Statusverlauf<Veroeffentlichungsstatus> herstellung() {
    return herstellung;
  }

  public Veroeffentlichungsstatus herstellungsstatus() {
    return herstellung.aktuell();
  }

  public void wechsleStatus(Veroeffentlichungsstatus neu, String akteur) {
    herstellung.protokolliere(neu, akteur);
  }

  public LocalDate datum() {
    return datum;
  }

  public void setDatum(LocalDate datum) {
    this.datum = datum;
  }

  public String verweis() {
    return verweis;
  }

  public void setVerweis(String verweis) {
    this.verweis = verweis;
  }

  // ── Dimension Akquise (reduzierter Vertrieb) ─────────────────────────────

  public Statusverlauf<Vertriebsstatus> akquise() {
    return akquise;
  }

  public Vertriebsstatus akquisestatus() {
    return akquise.aktuell();
  }

  public void wechsleVertrieb(Vertriebsstatus neu, String akteur) {
    akquise.protokolliere(neu, akteur);
  }

  /** Optionaler Auftraggebername; {@code null} bei Eigeninitiative. */
  public String auftraggeber() {
    return auftraggeber;
  }

  public void setAuftraggeber(String auftraggeber) {
    this.auftraggeber = auftraggeber;
  }
}
