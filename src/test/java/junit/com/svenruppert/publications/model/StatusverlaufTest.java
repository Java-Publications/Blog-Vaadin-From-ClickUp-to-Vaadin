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

package junit.com.svenruppert.publications.model;

import com.svenruppert.publications.model.Arbeitszustand;
import com.svenruppert.publications.model.Statuswechsel;
import com.svenruppert.publications.model.Statusverlauf;
import com.svenruppert.publications.model.Vertriebsstatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static com.svenruppert.publications.model.Arbeitszustand.BACKLOG;
import static com.svenruppert.publications.model.Arbeitszustand.DONE;
import static com.svenruppert.publications.model.Arbeitszustand.IN_PLANUNG;
import static com.svenruppert.publications.model.Arbeitszustand.IN_PROGRESS;
import static com.svenruppert.publications.model.Arbeitszustand.REVIEW;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Statusverlauf — append-only, geordnet über die Folgenummer")
class StatusverlaufTest {

  @Test
  @DisplayName("frischer Verlauf ist leer, aktuell() ist der Ausgangszustand")
  void frischerVerlaufIstLeer() {
    var v = new Statusverlauf<>(BACKLOG);
    assertTrue(v.isEmpty());
    assertEquals(0, v.anzahl());
    assertEquals(BACKLOG, v.aktuell());
    assertEquals(BACKLOG, v.initial());
    assertTrue(v.ereignisse().isEmpty());
  }

  @Test
  @DisplayName("protokolliere setzt aktuell() auf den neuen Zustand")
  void protokolliereSetztAktuell() {
    var v = new Statusverlauf<>(BACKLOG);
    v.protokolliere(IN_PLANUNG, "Sven");
    assertEquals(IN_PLANUNG, v.aktuell());
    assertEquals(1, v.anzahl());
  }

  @Test
  @DisplayName("von wird aus aktuell() abgeleitet — lückenlose Kette")
  void vonWirdAusAktuellAbgeleitet() {
    var v = new Statusverlauf<>(BACKLOG);
    v.protokolliere(IN_PLANUNG, "Sven");
    v.protokolliere(IN_PROGRESS, "Redaktion");
    v.protokolliere(REVIEW, "Gast-Autor");
    v.protokolliere(DONE, "Sven");

    List<Statuswechsel<Arbeitszustand>> e = v.ereignisse();
    assertEquals(4, e.size());
    // Kette: jedes von == nach des Vorgängers, erstes von == initial
    assertEquals(BACKLOG, e.get(0).von());
    for (int i = 1; i < e.size(); i++) {
      assertEquals(e.get(i - 1).nach(), e.get(i).von(),
          "von von Ereignis " + i + " muss nach des Vorgängers sein");
    }
    assertEquals(DONE, v.aktuell());
  }

  @Test
  @DisplayName("folge ist streng aufsteigend ab 0")
  void folgeIstStrengAufsteigendAbNull() {
    var v = new Statusverlauf<>(BACKLOG);
    v.protokolliere(IN_PLANUNG, "a");
    v.protokolliere(IN_PROGRESS, "b");
    v.protokolliere(REVIEW, "c");
    List<Statuswechsel<Arbeitszustand>> e = v.ereignisse();
    for (int i = 0; i < e.size(); i++) {
      assertEquals(i, e.get(i).folge());
    }
  }

  @Test
  @DisplayName("die Reihenfolge ruht auf der Folgenummer, nicht auf dem Zeitstempel")
  void reihenfolgeRuhtAufFolgeNichtAufZeitstempel() {
    var v = new Statusverlauf<>(BACKLOG);
    Instant spaet = Instant.parse("2026-06-10T09:00:00Z");
    Instant frueh = Instant.parse("2026-05-23T09:00:00Z");
    // bewusst mit rückläufigem Zeitstempel angehängt
    v.protokolliere(IN_PLANUNG, "a", spaet);
    v.protokolliere(IN_PROGRESS, "b", frueh);

    List<Statuswechsel<Arbeitszustand>> e = v.ereignisse();
    assertEquals(0, e.get(0).folge());
    assertEquals(1, e.get(1).folge());
    // aktuell folgt der Anhänge-Reihenfolge (Folge), nicht dem jüngsten Zeitstempel
    assertEquals(IN_PROGRESS, v.aktuell());
    assertEquals(spaet, e.get(0).zeitpunkt());
    assertEquals(frueh, e.get(1).zeitpunkt());
  }

  @Test
  @DisplayName("expliziter Zeitpunkt wird übernommen (Import/Test)")
  void expliziterZeitpunktWirdUebernommen() {
    var v = new Statusverlauf<>(BACKLOG);
    Instant t = Instant.parse("2026-05-23T09:00:00Z");
    v.protokolliere(IN_PLANUNG, "Sven", t);
    assertEquals(t, v.ereignisse().get(0).zeitpunkt());
  }

  @Test
  @DisplayName("ereignisse() ist eine unveränderliche Kopie")
  void ereignisseIstUnveraenderlicheKopie() {
    var v = new Statusverlauf<>(BACKLOG);
    v.protokolliere(IN_PLANUNG, "Sven");
    List<Statuswechsel<Arbeitszustand>> snapshot = v.ereignisse();
    assertThrows(UnsupportedOperationException.class,
        () -> snapshot.add(snapshot.get(0)));
    // weitere Wechsel verändern den bereits gezogenen Snapshot nicht
    v.protokolliere(IN_PROGRESS, "Sven");
    assertEquals(1, snapshot.size());
    assertEquals(2, v.anzahl());
  }

  @Test
  @DisplayName("Konstruktor lehnt null-Initial ab")
  void konstruktorLehntNullInitialAb() {
    assertThrows(NullPointerException.class, () -> new Statusverlauf<Arbeitszustand>(null));
  }

  @Test
  @DisplayName("protokolliere lehnt null-Zustand und null-Zeitpunkt ab")
  void protokolliereLehntNullAb() {
    var v = new Statusverlauf<>(BACKLOG);
    assertThrows(NullPointerException.class, () -> v.protokolliere(null, "Sven"));
    assertThrows(NullPointerException.class,
        () -> v.protokolliere(IN_PLANUNG, "Sven", null));
  }

  @Test
  @DisplayName("akteur darf null sein (Eigeninitiative/unbekannt)")
  void akteurDarfNullSein() {
    var v = new Statusverlauf<>(BACKLOG);
    v.protokolliere(IN_PLANUNG, null);
    assertEquals(IN_PLANUNG, v.aktuell());
    assertFalse(v.isEmpty());
  }

  @Test
  @DisplayName("der Verlaufstyp trägt jede Dimension gleichermaßen")
  void traegtJedeDimension() {
    var akquise = new Statusverlauf<>(Vertriebsstatus.REQUESTED);
    akquise.protokolliere(Vertriebsstatus.OFFERED, "Sven");
    akquise.protokolliere(Vertriebsstatus.ACCEPTED, "Sven");
    assertEquals(Vertriebsstatus.ACCEPTED, akquise.aktuell());
    assertEquals(2, akquise.anzahl());
  }
}
