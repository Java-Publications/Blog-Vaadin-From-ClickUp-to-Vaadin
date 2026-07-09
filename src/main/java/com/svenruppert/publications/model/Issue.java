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
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * The topic and the subject bracket, e.g. "Blog – Navigation – Coupled
 * navigation". Carries the technology/topic tags and its ordered sequence of
 * {@link Part}s. It is stateless itself — it is the bracket under which
 * everything else gathers. {@code origin} records the external id (e.g. a ClickUp
 * task) for the idempotent import.
 */
public final class Issue {

  private final UUID id;
  private String title;
  private String description;
  private final Set<Tag> tags = new LinkedHashSet<>();
  private final List<Part> parts = new ArrayList<>();
  private String origin;

  public Issue(String title) {
    this(UUID.randomUUID(), title);
  }

  public Issue(UUID id, String title) {
    this.id = Objects.requireNonNull(id, "id");
    setTitle(title);
  }

  public UUID id() {
    return id;
  }

  public String title() {
    return title;
  }

  public void setTitle(String title) {
    Objects.requireNonNull(title, "title");
    String stripped = title.strip();
    if (stripped.isEmpty()) {
      throw new IllegalArgumentException("Issue title must not be blank");
    }
    this.title = stripped;
  }

  /** Optional external id (import); {@code null} for a manually created issue. */
  public String origin() {
    return origin;
  }

  public void setOrigin(String origin) {
    this.origin = origin;
  }

  /** Optional original body (e.g. the ClickUp task text); {@code null} if none. */
  public String description() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  // ── tags ─────────────────────────────────────────────────────────────────

  public Set<Tag> tags() {
    return Set.copyOf(tags);
  }

  public void addTag(Tag tag) {
    tags.add(Objects.requireNonNull(tag, "tag"));
  }

  public void removeTag(Tag tag) {
    tags.remove(tag);
  }

  // ── parts ────────────────────────────────────────────────────────────────

  /** Creates a new part that gets the next position and {@code BACKLOG}. */
  public Part addPart() {
    Part part = new Part(parts.size() + 1);
    part.setIssue(this);
    parts.add(part);
    return part;
  }

  /** Appends an already-constructed part (import/reconstruction). */
  public Part adopt(Part part) {
    Objects.requireNonNull(part, "part");
    part.setIssue(this);
    parts.add(part);
    return part;
  }

  public List<Part> parts() {
    return List.copyOf(parts);
  }

  /** Parts ordered by their position. */
  public List<Part> partsInOrder() {
    List<Part> sorted = new ArrayList<>(parts);
    sorted.sort(Comparator.comparingInt(Part::position));
    return List.copyOf(sorted);
  }

  /**
   * Reorders the parts. {@code newOrder} must contain exactly the same parts; the
   * positions are reassigned 1..n. Affects only the content order, never the
   * status history.
   */
  public void reorderParts(List<Part> newOrder) {
    Objects.requireNonNull(newOrder, "newOrder");
    if (newOrder.size() != parts.size() || !parts.containsAll(newOrder)) {
      throw new IllegalArgumentException("newOrder must contain exactly the existing parts");
    }
    parts.clear();
    parts.addAll(newOrder);
    for (int i = 0; i < parts.size(); i++) {
      parts.get(i).setPosition(i + 1);
    }
  }
}
