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
import java.util.UUID;

/**
 * A client (Auftraggeber) — master data reduced to a name. A {@link Publication}
 * may reference one client (by name) as the party that commissioned it;
 * own-initiative publications have none. Maintained as a fixed, editable list so
 * the client is picked from a controlled set rather than typed freely.
 */
public final class Client {

  private final UUID id;
  private String name;

  public Client(String name) {
    this(UUID.randomUUID(), name);
  }

  public Client(UUID id, String name) {
    this.id = Objects.requireNonNull(id, "id");
    this.name = requireName(name);
  }

  private static String requireName(String name) {
    Objects.requireNonNull(name, "name");
    String stripped = name.strip();
    if (stripped.isEmpty()) {
      throw new IllegalArgumentException("Client name must not be blank");
    }
    return stripped;
  }

  public UUID id() {
    return id;
  }

  public String name() {
    return name;
  }

  public void setName(String name) {
    this.name = requireName(name);
  }

  @Override
  public String toString() {
    return name;
  }
}
