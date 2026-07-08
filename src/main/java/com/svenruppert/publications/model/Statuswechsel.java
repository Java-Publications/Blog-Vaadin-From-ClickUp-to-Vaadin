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
import java.util.Objects;

/**
 * Ein unveränderliches Statuswechsel-Ereignis innerhalb eines
 * {@link Statusverlauf}s.
 *
 * <p>Die {@code folge} beginnt je Verlauf bei {@code 0} und wächst mit jedem
 * Ereignis um eins; sie — nicht die Listenposition und nicht der
 * {@code zeitpunkt} — ist das maßgebliche Ordnungskriterium. Der
 * {@code zeitpunkt} bleibt beschreibend, da Uhren springen können.
 *
 * @param folge     streng aufsteigende Folgenummer, ab {@code 0}
 * @param von        Ausgangszustand (der {@code aktuell()}-Wert vor dem Wechsel)
 * @param nach       Zielzustand
 * @param akteur     Name des handelnden Akteurs (darf {@code null} sein)
 * @param zeitpunkt  beschreibender Zeitstempel des Wechsels
 */
public record Statuswechsel<S extends Enum<S>>(
    long folge, S von, S nach, String akteur, Instant zeitpunkt) {

  public Statuswechsel {
    if (folge < 0) {
      throw new IllegalArgumentException("folge must be >= 0, was " + folge);
    }
    Objects.requireNonNull(von, "von");
    Objects.requireNonNull(nach, "nach");
    Objects.requireNonNull(zeitpunkt, "zeitpunkt");
  }
}
