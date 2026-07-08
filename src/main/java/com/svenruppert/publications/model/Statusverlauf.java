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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Gehärteter, ausschließlich anhängender Statusverlauf einer einzelnen
 * Statusdimension. Kapselt die Ereignisliste vollständig: nach außen gibt es
 * nur eine unveränderliche Sicht ({@link #ereignisse()}) und als einzige
 * verändernde Fähigkeit das Anhängen ({@link #protokolliere}). Methoden zum
 * Einfügen, Entfernen oder Umsortieren gibt es nicht.
 *
 * <p>Weil {@code protokolliere} den Ausgangswert {@code von} selbst aus dem
 * aktuellen Zustand ableitet und die nächste Folgenummer vergibt, lässt sich
 * weder ein unstimmiger Übergang einschleusen noch die Reihenfolge verfälschen.
 * Der {@link #aktuell() aktuelle Zustand} ist stets das Ziel des letzten
 * Ereignisses und kann daher nicht von der Historie abweichen.
 *
 * <p>Der Typ trägt bewusst keinen {@link java.time.Clock} als Feld, damit der
 * persistierte Objektgraph frei von Laufzeit-Infrastruktur bleibt; der
 * Zeitstempel wird beim Anhängen erzeugt oder (Import, Test) explizit übergeben.
 *
 * @param <S> die Enum-Statusdimension
 */
public final class Statusverlauf<S extends Enum<S>> {

  private final S initial;
  private final List<Statuswechsel<S>> ereignisse = new ArrayList<>();

  /**
   * @param initial Ausgangszustand vor dem ersten Wechsel; niemals {@code null}
   */
  public Statusverlauf(S initial) {
    this.initial = Objects.requireNonNull(initial, "initial");
  }

  /** Ausgangszustand, mit dem der Verlauf initialisiert wurde. */
  public S initial() {
    return initial;
  }

  /**
   * @return das Ziel des letzten Ereignisses, sonst der Ausgangszustand
   */
  public S aktuell() {
    return ereignisse.isEmpty()
        ? initial
        : ereignisse.get(ereignisse.size() - 1).nach();
  }

  /** {@code true}, solange noch kein Wechsel protokolliert wurde. */
  public boolean isEmpty() {
    return ereignisse.isEmpty();
  }

  /** Anzahl bisher protokollierter Wechsel (zugleich die nächste Folgenummer). */
  public int anzahl() {
    return ereignisse.size();
  }

  /**
   * Hängt einen Statuswechsel an. {@code von} wird aus {@link #aktuell()}
   * abgeleitet, die {@code folge} ist die Anzahl bisheriger Ereignisse, der
   * {@code zeitpunkt} ist {@link Instant#now()}.
   *
   * @param neu    der neue Zustand; niemals {@code null}
   * @param akteur Name des Akteurs (darf {@code null} sein)
   */
  public void protokolliere(S neu, String akteur) {
    protokolliere(neu, akteur, Instant.now());
  }

  /**
   * Wie {@link #protokolliere(Enum, String)}, aber mit explizitem Zeitpunkt —
   * für den idempotenten Import (Übernahme des Originalzeitstempels) und für
   * deterministische Tests.
   */
  public void protokolliere(S neu, String akteur, Instant zeitpunkt) {
    Objects.requireNonNull(neu, "neu");
    Objects.requireNonNull(zeitpunkt, "zeitpunkt");
    S von = aktuell();
    ereignisse.add(new Statuswechsel<>(ereignisse.size(), von, neu, akteur, zeitpunkt));
  }

  /** Unveränderliche Sicht auf die Ereigniskette, geordnet nach Folgenummer. */
  public List<Statuswechsel<S>> ereignisse() {
    return List.copyOf(ereignisse);
  }
}
