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

import java.util.Objects;

/**
 * Technologie- oder Themen-Tag am {@link Issue}. Rein beschreibend,
 * beeinflusst keinen Status; dient als Filter- und Suchkriterium.
 */
public record Tag(String name) {
  public Tag {
    Objects.requireNonNull(name, "name");
    name = name.strip();
    if (name.isEmpty()) {
      throw new IllegalArgumentException("Tag name must not be blank");
    }
  }
}
