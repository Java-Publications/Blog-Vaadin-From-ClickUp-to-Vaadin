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

package com.svenruppert.publications.importetl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.svenruppert.dependencies.core.logger.HasLogger;
import com.svenruppert.publications.model.Arbeitszustand;
import com.svenruppert.publications.model.Issue;
import com.svenruppert.publications.model.Tag;
import com.svenruppert.publications.model.Teil;
import com.svenruppert.publications.persistence.PublicationsRepository;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tools.jackson.databind.ObjectMapper;

/**
 * ClickUp-Import als ETL. Die Extraktion zieht die rohen JSON-Antworten der
 * ClickUp-Schnittstelle einmalig; an Transformation und Laden lässt sich
 * anschließend beliebig oft gegen diese lokale Kopie iterieren. Der zentrale
 * Akt der Transformation ist die Verteilung des konflierten ClickUp-Status auf
 * die (hier redaktionelle) Dimension; das Laden ruht auf der vermerkten
 * {@link Issue#herkunft() Herkunft} und ist damit idempotent.
 *
 * <p>Wegwerfmodul für den Produktivbetrieb, in der Entwicklung ein Arbeitspferd.
 */
public final class ClickUpImportService implements HasLogger {

  private static final String API_BASE = "https://api.clickup.com/api/v2/list/";
  private static final String IMPORT_ACTOR = "clickup-import";

  private final ObjectMapper mapper = new ObjectMapper();

  /**
   * Zieht die Tasks einer ClickUp-Liste über die echte API und gibt die rohe
   * JSON-Antwort zurück. {@code token} ist das ClickUp-API-Token, {@code listId}
   * die Ziel-Liste.
   */
  public String extract(String token, String listId) throws IOException, InterruptedException {
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder(
            URI.create(API_BASE + listId + "/task?include_closed=true"))
        .header("Authorization", token)
        .header("Accept", "application/json")
        .GET()
        .build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() != 200) {
      throw new IllegalStateException(
          "ClickUp API returned HTTP " + response.statusCode());
    }
    logger().info("ClickUp extract: {} bytes from list {}", response.body().length(), listId);
    return response.body();
  }

  /**
   * Transformiert die rohe ClickUp-JSON und lädt sie idempotent in das
   * Repository. Ein bereits per Herkunft bekanntes Issue wird übersprungen — ein
   * Wiederholungslauf erzeugt daher keine Dubletten.
   */
  public ImportReport transformAndLoad(String rawJson, PublicationsRepository repo) {
    ClickUpList list = mapper.readValue(rawJson, ClickUpList.class);
    int angelegt = 0;
    int uebersprungen = 0;
    Map<String, Integer> verteilung = new LinkedHashMap<>();

    List<ClickUpTask> tasks = list.tasks() == null ? List.of() : list.tasks();
    for (ClickUpTask task : tasks) {
      String src = task.status() == null || task.status().status() == null
          ? "(none)" : task.status().status();
      Arbeitszustand zustand = mapStatus(src);
      verteilung.merge(src + " → " + zustand.name(), 1, Integer::sum);

      if (repo.findIssueByHerkunft(task.id()).isPresent()) {
        uebersprungen++;
        continue;
      }
      Issue issue = new Issue(task.name());
      issue.setHerkunft(task.id());
      if (task.tags() != null) {
        task.tags().stream()
            .filter(t -> t.name() != null && !t.name().isBlank())
            .forEach(t -> issue.addTag(new Tag(t.name())));
      }
      Teil teil = issue.addTeil();
      if (zustand != Arbeitszustand.BACKLOG) {
        teil.wechsleZustand(zustand, IMPORT_ACTOR);
      }
      repo.datenwurzel().addIssue(issue);
      angelegt++;
    }
    repo.persist();
    logger().info("ClickUp transform+load: {} created, {} skipped", angelegt, uebersprungen);
    return new ImportReport(angelegt, 0, uebersprungen, verteilung);
  }

  /** Verteilt den konflierten ClickUp-Status auf den redaktionellen Arbeitszustand. */
  public static Arbeitszustand mapStatus(String clickup) {
    String s = clickup == null ? "" : clickup.toLowerCase().strip();
    return switch (s) {
      case "in progress", "in-progress", "progress" -> Arbeitszustand.IN_PROGRESS;
      case "review", "in review" -> Arbeitszustand.REVIEW;
      case "planning", "in planning", "planned", "to do", "todo" -> Arbeitszustand.IN_PLANUNG;
      case "done", "complete", "completed", "closed", "published" -> Arbeitszustand.DONE;
      case "skipped" -> Arbeitszustand.SKIPPED;
      case "cancelled", "canceled" -> Arbeitszustand.CANCELLED;
      default -> Arbeitszustand.BACKLOG;
    };
  }

  // ── ClickUp JSON shape (only the fields we consume) ───────────────────────

  @JsonIgnoreProperties(ignoreUnknown = true)
  record ClickUpList(List<ClickUpTask> tasks) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  record ClickUpTask(String id, String name, ClickUpStatus status,
                     List<ClickUpTag> tags, String url) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  record ClickUpStatus(String status) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  record ClickUpTag(String name) {
  }
}
