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

import com.svenruppert.publications.model.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TagTest {

  @Test
  void nameIsStripped() {
    assertEquals("Vaadin", new Tag("  Vaadin  ").name());
  }

  @Test
  void equalNameEqualTag() {
    assertEquals(new Tag("Java"), new Tag("Java"));
  }

  @Test
  void blankNameIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> new Tag("   "));
    assertThrows(IllegalArgumentException.class, () -> new Tag(""));
  }

  @Test
  void nullNameIsRejected() {
    assertThrows(NullPointerException.class, () -> new Tag(null));
  }
}
