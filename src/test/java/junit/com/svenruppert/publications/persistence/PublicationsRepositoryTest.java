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

package junit.com.svenruppert.publications.persistence;

import com.svenruppert.publications.model.Issue;
import com.svenruppert.publications.model.Language;
import com.svenruppert.publications.model.PublicationPlace;
import com.svenruppert.publications.persistence.InMemoryPublicationsPersistence;
import com.svenruppert.publications.persistence.PublicationsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublicationsRepositoryTest {

  private PublicationsRepository repo;

  @BeforeEach
  void setUp() {
    repo = new PublicationsRepository(new InMemoryPublicationsPersistence());
  }

  @Test
  void createIssueAndFind() {
    Issue issue = repo.createIssue("Blog – Navigation – Coupled navigation");
    assertEquals(1, repo.issues().size());
    assertTrue(repo.findIssue(issue.id()).isPresent());
    assertEquals(issue, repo.findIssue(issue.id()).orElseThrow());
  }

  @Test
  void findIssueByOrigin() {
    Issue issue = repo.createIssue("Topic");
    issue.setOrigin("CU-8842");
    repo.persist();
    assertTrue(repo.findIssueByOrigin("CU-8842").isPresent());
    assertFalse(repo.findIssueByOrigin("CU-0000").isPresent());
    assertFalse(repo.findIssueByOrigin(null).isPresent());
  }

  @Test
  void placesForLanguageFiltersByLanguageRule() {
    repo.createPublicationPlace("svenruppert.com", Set.of(Language.GERMAN, Language.ENGLISH));
    repo.createPublicationPlace("DZone", Set.of(Language.ENGLISH));
    List<PublicationPlace> de = repo.placesFor(Language.GERMAN);
    assertEquals(1, de.size());
    assertEquals("svenruppert.com", de.get(0).name());
    assertEquals(2, repo.placesFor(Language.ENGLISH).size());
  }

  @Test
  void deleteIssueRemoves() {
    Issue a = repo.createIssue("A");
    repo.createIssue("B");
    repo.deleteIssue(a);
    assertEquals(1, repo.issues().size());
    assertFalse(repo.findIssue(a.id()).isPresent());
  }

  @Test
  void findPartAndPublicationAcrossTheGraph() {
    var place = repo.createPublicationPlace("svenruppert.com", Set.of(Language.GERMAN));
    Issue issue = repo.createIssue("Topic");
    var part = issue.addPart();
    var version = part.addLanguageVersion(Language.GERMAN);
    var v = version.planPublication(place);
    repo.persist();

    assertTrue(repo.findPart(part.id()).isPresent());
    assertTrue(repo.findVersion(version.id()).isPresent());
    assertTrue(repo.findPublication(v.id()).isPresent());
    assertEquals(1, repo.allPublications().size());
  }
}
