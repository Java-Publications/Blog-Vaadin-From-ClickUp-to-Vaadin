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

package com.svenruppert.publications.persistence;

import java.util.Objects;

/**
 * Lazy-Holder-Singleton für das {@link PublicationsRepository} mit Test-Seam —
 * analog zu {@code UserDirectoryProvider}. Das Eclipse-Store-gestützte
 * Repository wird erst beim ersten Zugriff geöffnet, nicht beim Klassenladen;
 * Tests setzen vorher via {@link #setRepository} eine In-Memory-Variante.
 */
public final class PublicationsProvider {

  private static volatile PublicationsRepository override;

  private PublicationsProvider() {
  }

  private static final class Holder {
    static final PublicationsRepository INSTANCE = new PublicationsRepository();
  }

  public static PublicationsRepository repository() {
    PublicationsRepository swap = override;
    return swap != null ? swap : Holder.INSTANCE;
  }

  public static void setRepository(PublicationsRepository replacement) {
    override = Objects.requireNonNull(replacement, "replacement");
  }

  public static void reset() {
    override = null;
  }
}
