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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Ein einziger Wurzelknoten, der die beiden verbliebenen Aggregatlisten hält.
 * Dient zugleich als Objektwurzel für die Eclipse-Store-Persistenz der Domäne.
 */
public final class Datenwurzel {

  private final List<Issue> issues = new ArrayList<>();
  private final List<Publikationsort> publikationsorte = new ArrayList<>();

  public List<Issue> issues() {
    return List.copyOf(issues);
  }

  public Issue addIssue(Issue issue) {
    issues.add(Objects.requireNonNull(issue, "issue"));
    return issue;
  }

  public void removeIssue(Issue issue) {
    issues.remove(issue);
  }

  public List<Publikationsort> publikationsorte() {
    return List.copyOf(publikationsorte);
  }

  public Publikationsort addPublikationsort(Publikationsort ort) {
    publikationsorte.add(Objects.requireNonNull(ort, "ort"));
    return ort;
  }

  public void removePublikationsort(Publikationsort ort) {
    publikationsorte.remove(ort);
  }
}
