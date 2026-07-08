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

import com.svenruppert.publications.model.Issue;
import com.svenruppert.publications.model.Publikationsort;
import com.svenruppert.publications.model.Sprache;
import com.svenruppert.publications.model.Sprachfassung;
import com.svenruppert.publications.model.Veroeffentlichung;
import com.svenruppert.publications.model.Veroeffentlichungsstatus;
import com.svenruppert.publications.model.Vertriebsstatus;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SprachregelTest {

  private Sprachfassung fassung(Sprache sprache) {
    return new Issue("Thema").addTeil().addSprachfassung(sprache);
  }

  @Test
  void publikationsortUnterstuetztSeineSprachen() {
    var ort = new Publikationsort("DZone", Set.of(Sprache.ENGLISCH));
    assertTrue(ort.unterstuetzt(Sprache.ENGLISCH));
    assertFalse(ort.unterstuetzt(Sprache.DEUTSCH));
  }

  @Test
  void planenNurAufSprachlichZulaessigenOrt() {
    var de = fassung(Sprache.DEUTSCH);
    var nurEnglisch = new Publikationsort("DZone", Set.of(Sprache.ENGLISCH));
    var deUndEn = new Publikationsort("svenruppert.com", Set.of(Sprache.DEUTSCH, Sprache.ENGLISCH));

    assertFalse(de.kannVeroeffentlichtWerdenAn(nurEnglisch));
    assertTrue(de.kannVeroeffentlichtWerdenAn(deUndEn));

    assertThrows(IllegalArgumentException.class, () -> de.planeVeroeffentlichung(nurEnglisch));
    Veroeffentlichung v = de.planeVeroeffentlichung(deUndEn);
    assertEquals(deUndEn, v.ort());
    assertEquals(Sprache.DEUTSCH, v.sprache());
    assertEquals(1, de.veroeffentlichungen().size());
  }

  @Test
  void neueVeroeffentlichungStartetInDenAusgangszustaenden() {
    var de = fassung(Sprache.DEUTSCH);
    var ort = new Publikationsort("svenruppert.com", Set.of(Sprache.DEUTSCH));
    Veroeffentlichung v = de.planeVeroeffentlichung(ort);
    assertEquals(Veroeffentlichungsstatus.PLANNED, v.herstellungsstatus());
    assertEquals(Vertriebsstatus.REQUESTED, v.akquisestatus());
  }

  @Test
  void herstellungUndAkquiseLaufenOrthogonal() {
    var de = fassung(Sprache.DEUTSCH);
    var ort = new Publikationsort("svenruppert.com", Set.of(Sprache.DEUTSCH));
    Veroeffentlichung v = de.planeVeroeffentlichung(ort);

    // Zusage liegt vor, während Herstellung erst vorbereitet ist
    v.wechsleVertrieb(Vertriebsstatus.OFFERED, "Sven");
    v.wechsleVertrieb(Vertriebsstatus.ACCEPTED, "Sven");
    v.wechsleStatus(Veroeffentlichungsstatus.PREPARED, "Sven");

    assertEquals(Vertriebsstatus.ACCEPTED, v.akquisestatus());
    assertEquals(Veroeffentlichungsstatus.PREPARED, v.herstellungsstatus());
    // beide Ketten unabhängig geführt
    assertEquals(2, v.akquise().anzahl());
    assertEquals(1, v.herstellung().anzahl());
  }

  @Test
  void setOrtPrueftSprachregelErneut() {
    var de = fassung(Sprache.DEUTSCH);
    var deOrt = new Publikationsort("svenruppert.com", Set.of(Sprache.DEUTSCH));
    var enOrt = new Publikationsort("DZone", Set.of(Sprache.ENGLISCH));
    Veroeffentlichung v = de.planeVeroeffentlichung(deOrt);
    assertThrows(IllegalArgumentException.class, () -> v.setOrt(enOrt));
  }

  @Test
  void aktualisierungAlsRuecksprungNachPublished() {
    var de = fassung(Sprache.DEUTSCH);
    var ort = new Publikationsort("svenruppert.com", Set.of(Sprache.DEUTSCH));
    Veroeffentlichung v = de.planeVeroeffentlichung(ort);
    v.wechsleStatus(Veroeffentlichungsstatus.PREPARED, "Sven");
    v.wechsleStatus(Veroeffentlichungsstatus.PUBLISHED, "Sven");
    v.wechsleStatus(Veroeffentlichungsstatus.UPDATE_NEEDED, "Sven");
    assertEquals(Veroeffentlichungsstatus.UPDATE_NEEDED, v.herstellungsstatus());
    assertEquals(3, v.herstellung().anzahl());
  }
}
