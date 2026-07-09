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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Connects a {@link LanguageVersion} with a {@link PublicationPlace} and carries
 * <em>two</em> orthogonal lifecycles at once: production ({@link ProductionStatus},
 * initial state {@code PLANNED}) and the reduced acquisition
 * ({@link AcquisitionStatus}, initial state {@code REQUESTED}). The editorial
 * state stays on the {@link Part}.
 *
 * <p>The language of a publication is always that of its version; the
 * <em>language rule</em> (the place must support that language) is enforced as an
 * invariant in the constructor.
 */
public final class Publication {

  private final UUID id;
  private final LanguageVersion version;
  private PublicationPlace place;

  private final StatusHistory<ProductionStatus> production;
  private LocalDate date;
  private String link;

  private final StatusHistory<AcquisitionStatus> acquisition;
  private String client;

  public Publication(LanguageVersion version, PublicationPlace place) {
    this(UUID.randomUUID(), version, place);
  }

  public Publication(UUID id, LanguageVersion version, PublicationPlace place) {
    this.id = Objects.requireNonNull(id, "id");
    this.version = Objects.requireNonNull(version, "version");
    this.place = checkLanguageRule(version, Objects.requireNonNull(place, "place"));
    this.production = new StatusHistory<>(ProductionStatus.PLANNED);
    this.acquisition = new StatusHistory<>(AcquisitionStatus.REQUESTED);
  }

  private static PublicationPlace checkLanguageRule(LanguageVersion version, PublicationPlace place) {
    if (!place.supports(version.language())) {
      throw new IllegalArgumentException(
          "Language rule violated: place '" + place.name() + "' does not support "
              + version.language());
    }
    return place;
  }

  public UUID id() {
    return id;
  }

  @SuppressFBWarnings(value = "EI_EXPOSE_REP",
      justification = "intentional aggregate reference — the publication shares its language "
          + "version by identity within the object graph, never a copy")
  public LanguageVersion version() {
    return version;
  }

  /** The language of the publication is always that of its version. */
  public Language language() {
    return version.language();
  }

  @SuppressFBWarnings(value = "EI_EXPOSE_REP",
      justification = "intentional aggregate reference — the publication shares its place by "
          + "identity within the object graph, never a copy")
  public PublicationPlace place() {
    return place;
  }

  /** Change the place, re-checking the language rule. */
  public void setPlace(PublicationPlace newPlace) {
    this.place = checkLanguageRule(version, Objects.requireNonNull(newPlace, "newPlace"));
  }

  // ── Production dimension ─────────────────────────────────────────────────

  public StatusHistory<ProductionStatus> production() {
    return production;
  }

  public ProductionStatus productionStatus() {
    return production.current();
  }

  public void changeProductionStatus(ProductionStatus to, String actor) {
    production.record(to, actor);
  }

  public LocalDate date() {
    return date;
  }

  public void setDate(LocalDate date) {
    this.date = date;
  }

  public String link() {
    return link;
  }

  public void setLink(String link) {
    this.link = link;
  }

  // ── Acquisition dimension (reduced sales) ────────────────────────────────

  public StatusHistory<AcquisitionStatus> acquisition() {
    return acquisition;
  }

  public AcquisitionStatus acquisitionStatus() {
    return acquisition.current();
  }

  public void changeAcquisitionStatus(AcquisitionStatus to, String actor) {
    acquisition.record(to, actor);
  }

  /** Optional client name; {@code null} for own initiative. */
  public String client() {
    return client;
  }

  public void setClient(String client) {
    this.client = client;
  }
}
