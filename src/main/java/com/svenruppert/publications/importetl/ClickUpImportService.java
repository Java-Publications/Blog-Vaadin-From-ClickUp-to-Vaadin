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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.svenruppert.dependencies.core.logger.HasLogger;
import com.svenruppert.dependencies.core.net.HttpStatus;
import com.svenruppert.dependencies.core.net.MediaType;
import com.svenruppert.publications.model.EditorialState;
import com.svenruppert.publications.model.Issue;
import com.svenruppert.publications.model.Part;
import com.svenruppert.publications.model.Tag;
import com.svenruppert.publications.persistence.PublicationsRepository;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * ClickUp import as ETL. Extraction pulls the raw JSON responses of the ClickUp
 * API once; transformation and loading can then iterate against that local copy
 * arbitrarily often. The central act of the transformation is distributing the
 * conflated ClickUp status onto the (here editorial) dimension; loading rests on
 * the recorded {@link Issue#origin() origin} and is therefore idempotent.
 *
 * <p>A throwaway module for production use, a workhorse in development.
 */
public final class ClickUpImportService implements HasLogger {

  private static final String API_BASE = "https://api.clickup.com/api/v2/list/";
  private static final String IMPORT_ACTOR = "clickup-import";

  /** Hard cap on pages fetched, so a misbehaving API can never loop forever. */
  private static final int MAX_PAGES = 1000;

  private final ObjectMapper mapper = new ObjectMapper();

  /**
   * Pulls <em>all</em> tasks of a ClickUp list through the real API and returns
   * them as one combined {@code {"tasks":[…]}} JSON document. The ClickUp task
   * endpoint is paginated at 100 tasks per page, so this walks {@code ?page=0,1,…}
   * until {@code last_page} (or an empty page), concatenating the raw task nodes —
   * every original field is preserved for the transform/load and the local cache.
   * {@code token} is the ClickUp API token, {@code listId} the target list.
   */
  public String extract(String token, String listId) throws IOException, InterruptedException {
    HttpClient client = HttpClient.newHttpClient();
    ArrayNode allTasks = mapper.createArrayNode();
    int page = 0;
    boolean lastPage = false;
    while (!lastPage && page < MAX_PAGES) {
      HttpRequest request = HttpRequest.newBuilder(
              URI.create(API_BASE + listId + "/task?include_closed=true&page=" + page))
          .header("Authorization", token)
          .header("Accept", MediaType.APPLICATION_JSON.mime())
          .GET()
          .build();
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != HttpStatus.OK.code()) {
        throw new IllegalStateException(
            "ClickUp API returned HTTP " + response.statusCode() + " on page " + page);
      }
      JsonNode root = mapper.readTree(response.body());
      JsonNode tasks = root.get("tasks");
      int onPage = 0;
      if (tasks != null && tasks.isArray()) {
        for (JsonNode task : tasks) {
          allTasks.add(task);
          onPage++;
        }
      }
      lastPage = root.path("last_page").asBoolean(false) || onPage == 0;
      logger().info("ClickUp extract: list {} page {} → {} tasks ({} total so far)",
          listId, page, onPage, allTasks.size());
      page++;
    }
    ObjectNode combined = mapper.createObjectNode();
    combined.set("tasks", allTasks);
    String json = mapper.writeValueAsString(combined);
    logger().info("ClickUp extract: {} tasks across {} page(s) from list {} ({} bytes)",
        allTasks.size(), page, listId, json.length());
    return json;
  }

  /**
   * Reports transform/load progress so a UI can drive a determinate progress bar.
   * {@code done} counts the tasks processed so far, {@code total} the tasks in the
   * extract.
   */
  @FunctionalInterface
  public interface ProgressListener {
    void onProgress(int done, int total);

    /** A listener that ignores every update. */
    ProgressListener NONE = (done, total) -> { };
  }

  /**
   * Transforms the raw ClickUp JSON and loads it idempotently into the
   * repository. An issue already known by its origin is skipped — a repeat run
   * therefore creates no duplicates.
   */
  public ImportReport transformAndLoad(String rawJson, PublicationsRepository repo) {
    return transformAndLoad(rawJson, repo, ProgressListener.NONE);
  }

  /**
   * As {@link #transformAndLoad(String, PublicationsRepository)}, but reports
   * per-task progress to {@code listener} (the final {@link #load persist} runs
   * once the loop completes).
   */
  public ImportReport transformAndLoad(String rawJson, PublicationsRepository repo,
                                       ProgressListener listener) {
    ClickUpList list = mapper.readValue(rawJson, ClickUpList.class);
    int created = 0;
    int skipped = 0;
    Map<String, Integer> distribution = new LinkedHashMap<>();

    List<ClickUpTask> tasks = list.tasks() == null ? List.of() : list.tasks();
    int total = tasks.size();
    logger().info("ClickUp transform+load: processing {} tasks", total);
    for (int idx = 0; idx < total; idx++) {
      ClickUpTask task = tasks.get(idx);
      String src = task.status() == null || task.status().status() == null
          ? "(none)" : task.status().status();
      EditorialState state = mapStatus(src);
      distribution.merge(src + " → " + state.name(), 1, Integer::sum);

      if (repo.findIssueByOrigin(task.id()).isPresent()) {
        skipped++;
      } else {
        Issue issue = new Issue(task.name());
        issue.setOrigin(task.id());
        issue.setDescription(task.content());
        if (task.tags() != null) {
          task.tags().stream()
              .filter(t -> t.name() != null && !t.name().isBlank())
              .forEach(t -> issue.addTag(new Tag(t.name())));
        }
        Part part = issue.addPart();
        if (state != EditorialState.BACKLOG) {
          part.changeState(state, IMPORT_ACTOR);
        }
        repo.dataRoot().addIssue(issue);
        created++;
      }
      listener.onProgress(idx + 1, total);
    }
    repo.persist();
    logger().info("ClickUp transform+load: {} created, {} skipped", created, skipped);
    return new ImportReport(created, 0, skipped, distribution);
  }

  /** Distributes the conflated ClickUp status onto the editorial state. */
  public static EditorialState mapStatus(String clickup) {
    String s = clickup == null ? "" : clickup.toLowerCase().strip();
    return switch (s) {
      case "in progress", "in-progress", "progress" -> EditorialState.IN_PROGRESS;
      case "review", "in review" -> EditorialState.REVIEW;
      case "planning", "in planning", "planned", "to do", "todo" -> EditorialState.IN_PLANNING;
      case "done", "complete", "completed", "closed", "published" -> EditorialState.DONE;
      case "skipped" -> EditorialState.SKIPPED;
      case "cancelled", "canceled" -> EditorialState.CANCELLED;
      default -> EditorialState.BACKLOG;
    };
  }

  // ── ClickUp JSON shape (only the fields we consume) ───────────────────────

  @JsonIgnoreProperties(ignoreUnknown = true)
  record ClickUpList(List<ClickUpTask> tasks) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  record ClickUpTask(String id, String name, ClickUpStatus status,
                     List<ClickUpTag> tags, String url,
                     String description,
                     @JsonProperty("text_content") String textContent) {

    /** The task's original body — plain {@code text_content} preferred, else markdown. */
    String content() {
      if (textContent != null && !textContent.isBlank()) {
        return textContent;
      }
      if (description != null && !description.isBlank()) {
        return description;
      }
      return null;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  record ClickUpStatus(String status) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  record ClickUpTag(String name) {
  }
}
