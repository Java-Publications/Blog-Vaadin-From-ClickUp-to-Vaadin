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

import com.svenruppert.publications.persistence.InMemoryPublicationsPersistence;
import com.svenruppert.publications.persistence.PublicationsProvider;
import com.svenruppert.publications.persistence.PublicationsRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

class PublicationsProviderTest {

  @AfterEach
  void tearDown() {
    PublicationsProvider.reset();
  }

  @Test
  void overrideWirdZurueckgegeben() {
    var repo = new PublicationsRepository(new InMemoryPublicationsPersistence());
    PublicationsProvider.setRepository(repo);
    assertSame(repo, PublicationsProvider.repository());
  }

  @Test
  void resetErlaubtEinNeuesOverride() {
    var a = new PublicationsRepository(new InMemoryPublicationsPersistence());
    PublicationsProvider.setRepository(a);
    assertSame(a, PublicationsProvider.repository());

    PublicationsProvider.reset();

    // ohne den Lazy-Holder (echter Store) auszulösen: ein neues Override greift
    var b = new PublicationsRepository(new InMemoryPublicationsPersistence());
    PublicationsProvider.setRepository(b);
    assertSame(b, PublicationsProvider.repository());
  }
}
