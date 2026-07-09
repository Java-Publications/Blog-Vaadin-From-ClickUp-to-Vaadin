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
 * A single root node holding the two remaining aggregate lists. Also serves as
 * the object root for the domain's Eclipse-Store persistence.
 */
public final class DataRoot {

  private final List<Issue> issues = new ArrayList<>();
  private final List<PublicationPlace> publicationPlaces = new ArrayList<>();

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

  public List<PublicationPlace> publicationPlaces() {
    return List.copyOf(publicationPlaces);
  }

  public PublicationPlace addPublicationPlace(PublicationPlace place) {
    publicationPlaces.add(Objects.requireNonNull(place, "place"));
    return place;
  }

  public void removePublicationPlace(PublicationPlace place) {
    publicationPlaces.remove(place);
  }
}
