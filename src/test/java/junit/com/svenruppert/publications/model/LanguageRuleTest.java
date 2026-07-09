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

import com.svenruppert.publications.model.AcquisitionStatus;
import com.svenruppert.publications.model.Issue;
import com.svenruppert.publications.model.Language;
import com.svenruppert.publications.model.LanguageVersion;
import com.svenruppert.publications.model.ProductionStatus;
import com.svenruppert.publications.model.Publication;
import com.svenruppert.publications.model.PublicationPlace;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LanguageRuleTest {

  private LanguageVersion version(Language language) {
    return new Issue("Topic").addPart().addLanguageVersion(language);
  }

  @Test
  void placeSupportsItsLanguages() {
    var place = new PublicationPlace("DZone", Set.of(Language.ENGLISH));
    assertTrue(place.supports(Language.ENGLISH));
    assertFalse(place.supports(Language.GERMAN));
  }

  @Test
  void planningOnlyAtALanguageAdmissiblePlace() {
    var de = version(Language.GERMAN);
    var englishOnly = new PublicationPlace("DZone", Set.of(Language.ENGLISH));
    var deAndEn = new PublicationPlace("svenruppert.com", Set.of(Language.GERMAN, Language.ENGLISH));

    assertFalse(de.canBePublishedAt(englishOnly));
    assertTrue(de.canBePublishedAt(deAndEn));

    assertThrows(IllegalArgumentException.class, () -> de.planPublication(englishOnly));
    Publication v = de.planPublication(deAndEn);
    assertEquals(deAndEn, v.place());
    assertEquals(Language.GERMAN, v.language());
    assertEquals(1, de.publications().size());
  }

  @Test
  void newPublicationStartsInTheInitialStates() {
    var de = version(Language.GERMAN);
    var place = new PublicationPlace("svenruppert.com", Set.of(Language.GERMAN));
    Publication v = de.planPublication(place);
    assertEquals(ProductionStatus.PLANNED, v.productionStatus());
    assertEquals(AcquisitionStatus.REQUESTED, v.acquisitionStatus());
  }

  @Test
  void productionAndAcquisitionRunOrthogonally() {
    var de = version(Language.GERMAN);
    var place = new PublicationPlace("svenruppert.com", Set.of(Language.GERMAN));
    Publication v = de.planPublication(place);

    v.changeAcquisitionStatus(AcquisitionStatus.OFFERED, "Sven");
    v.changeAcquisitionStatus(AcquisitionStatus.ACCEPTED, "Sven");
    v.changeProductionStatus(ProductionStatus.PREPARED, "Sven");

    assertEquals(AcquisitionStatus.ACCEPTED, v.acquisitionStatus());
    assertEquals(ProductionStatus.PREPARED, v.productionStatus());
    assertEquals(2, v.acquisition().count());
    assertEquals(1, v.production().count());
  }

  @Test
  void setPlaceReChecksTheLanguageRule() {
    var de = version(Language.GERMAN);
    var dePlace = new PublicationPlace("svenruppert.com", Set.of(Language.GERMAN));
    var enPlace = new PublicationPlace("DZone", Set.of(Language.ENGLISH));
    Publication v = de.planPublication(dePlace);
    assertThrows(IllegalArgumentException.class, () -> v.setPlace(enPlace));
  }

  @Test
  void updateAsAReturnStepAfterPublished() {
    var de = version(Language.GERMAN);
    var place = new PublicationPlace("svenruppert.com", Set.of(Language.GERMAN));
    Publication v = de.planPublication(place);
    v.changeProductionStatus(ProductionStatus.PREPARED, "Sven");
    v.changeProductionStatus(ProductionStatus.PUBLISHED, "Sven");
    v.changeProductionStatus(ProductionStatus.UPDATE_NEEDED, "Sven");
    assertEquals(ProductionStatus.UPDATE_NEEDED, v.productionStatus());
    assertEquals(3, v.production().count());
  }
}
