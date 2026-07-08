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

package com.svenruppert.publications.importetl;

import java.util.Map;

/**
 * Ergebnis eines Transform-und-Lade-Laufs.
 *
 * @param angelegt        neu erzeugte Issues
 * @param aktualisiert    aktualisierte Issues (idempotenter Re-Import ändert nichts)
 * @param uebersprungen   bereits vorhandene Issues (per Herkunft erkannt)
 * @param statusVerteilung Abbildung des konflierten ClickUp-Status auf die
 *                          entflochtene Dimension, gezählt (für die Konsole)
 */
public record ImportReport(
    int angelegt,
    int aktualisiert,
    int uebersprungen,
    Map<String, Integer> statusVerteilung) {

  public int gesamt() {
    return angelegt + aktualisiert + uebersprungen;
  }
}
