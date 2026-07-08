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
 * Das Thema und die fachliche Klammer, etwa „Blog – Navigation –
 * Koppelnavigation". Trägt die Technologie-/Themen-Tags und seine geordnete
 * Folge von {@link Teil}en. Selbst zustandslos — es ist die Klammer, unter der
 * sich alles Weitere sammelt. {@code herkunft} vermerkt die externe Kennung
 * (z. B. ClickUp-Task) für den idempotenten Import.
 */
public final class Issue {

  private final UUID id;
  private String titel;
  private final Set<Tag> tags = new LinkedHashSet<>();
  private final List<Teil> teile = new ArrayList<>();
  private String herkunft;

  public Issue(String titel) {
    this(UUID.randomUUID(), titel);
  }

  public Issue(UUID id, String titel) {
    this.id = Objects.requireNonNull(id, "id");
    setTitel(titel);
  }

  public UUID id() {
    return id;
  }

  public String titel() {
    return titel;
  }

  public void setTitel(String titel) {
    Objects.requireNonNull(titel, "titel");
    String stripped = titel.strip();
    if (stripped.isEmpty()) {
      throw new IllegalArgumentException("Issue titel must not be blank");
    }
    this.titel = stripped;
  }

  /** Optionale externe Kennung (Import); {@code null} bei manueller Anlage. */
  public String herkunft() {
    return herkunft;
  }

  public void setHerkunft(String herkunft) {
    this.herkunft = herkunft;
  }

  // ── Tags ───────────────────────────────────────────────────────────────

  public Set<Tag> tags() {
    return Set.copyOf(tags);
  }

  public void addTag(Tag tag) {
    tags.add(Objects.requireNonNull(tag, "tag"));
  }

  public void removeTag(Tag tag) {
    tags.remove(tag);
  }

  // ── Teile ──────────────────────────────────────────────────────────────

  /** Legt einen neuen Teil an, der die nächste Position und {@code BACKLOG} erhält. */
  public Teil addTeil() {
    Teil teil = new Teil(teile.size() + 1);
    teil.setIssue(this);
    teile.add(teil);
    return teil;
  }

  /** Hängt einen bereits konstruierten Teil an (Import/Rekonstruktion). */
  public Teil uebernimm(Teil teil) {
    Objects.requireNonNull(teil, "teil");
    teil.setIssue(this);
    teile.add(teil);
    return teil;
  }

  public List<Teil> teile() {
    return List.copyOf(teile);
  }

  /** Teile geordnet nach ihrer Reihenfolge. */
  public List<Teil> teileInReihenfolge() {
    List<Teil> sorted = new ArrayList<>(teile);
    sorted.sort(Comparator.comparingInt(Teil::reihenfolge));
    return List.copyOf(sorted);
  }

  /**
   * Ordnet die Teile neu. {@code neueOrdnung} muss genau dieselben Teile
   * enthalten; die Reihenfolge-Nummern werden auf 1..n neu vergeben. Betrifft
   * ausschließlich die inhaltliche Ordnung, niemals die Statushistorie.
   */
  public void ordneTeileNeu(List<Teil> neueOrdnung) {
    Objects.requireNonNull(neueOrdnung, "neueOrdnung");
    if (neueOrdnung.size() != teile.size() || !teile.containsAll(neueOrdnung)) {
      throw new IllegalArgumentException("neueOrdnung must contain exactly the existing Teile");
    }
    teile.clear();
    teile.addAll(neueOrdnung);
    for (int i = 0; i < teile.size(); i++) {
      teile.get(i).setReihenfolge(i + 1);
    }
  }
}
