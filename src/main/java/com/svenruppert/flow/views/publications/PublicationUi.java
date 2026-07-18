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

package com.svenruppert.flow.views.publications;

import com.svenruppert.flow.i18n.I18n;
import com.svenruppert.publications.model.AcquisitionStatus;
import com.svenruppert.publications.model.EditorialState;
import com.svenruppert.publications.model.Part;
import com.svenruppert.publications.model.ProductionStatus;
import com.svenruppert.publications.model.Tag;
import com.vaadin.flow.component.html.Span;

import java.util.Set;

/**
 * A small collection of reusable Lumo badges for the publications views. Maps the
 * status values of the three dimensions onto the Lumo badge theme variants
 * (success/error/contrast), so all views (topics workspace, editorial board,
 * publication view, history) share the same color language.
 */
public final class PublicationUi {

  private PublicationUi() {
  }

  /** A Lumo badge with an optional color variant ({@code "success"} …). */
  public static Span badge(String text, String variant) {
    Span span = new Span(text);
    span.getElement().getThemeList().add("badge");
    span.getElement().getThemeList().add("small");
    if (variant != null && !variant.isBlank()) {
      span.getElement().getThemeList().add(variant);
    }
    return span;
  }

  /** Neutral tag badge. */
  public static Span tag(String name) {
    return badge(name, "contrast");
  }

  /**
   * Whether {@code issueTags} satisfies the {@code wanted} tag selection: with
   * {@code requireAll} the issue must carry <em>all</em> selected tags (AND),
   * otherwise <em>any</em> of them (OR). An empty selection always matches. Pure
   * and shared by the topic workspace and the editorial board so the AND/OR
   * semantics stay identical.
   */
  public static boolean matchesTags(Set<Tag> issueTags, Set<Tag> wanted, boolean requireAll) {
    if (wanted.isEmpty()) {
      return true;
    }
    return requireAll ? issueTags.containsAll(wanted) : issueTags.stream().anyMatch(wanted::contains);
  }

  /**
   * Human-readable "blog post" label for a {@link Part} — the owning topic title
   * plus the part number, e.g. {@code "Blog – Navigation · Part 2"}. Used to tell
   * the user which post a publication or version belongs to. {@code null}-safe.
   */
  public static String blogPost(Part part) {
    if (part == null) {
      return I18n.tr("pub.unknownPost", "(unknown topic)");
    }
    String topic = part.issue() != null ? part.issue().title() : "?";
    return topic + " · " + I18n.tr("themen.part", "Part {0}", part.position());
  }

  public static Span editorialState(EditorialState state) {
    return badge(state.name(), switch (state) {
      case DONE -> "success";
      case CANCELLED -> "error";
      case IN_PROGRESS, IN_PLANNING, REVIEW -> "contrast";
      case BACKLOG, SKIPPED -> "";
    });
  }

  public static Span production(ProductionStatus status) {
    return badge(status.name(), switch (status) {
      case PUBLISHED -> "success";
      case UPDATE_NEEDED -> "error";
      case PREPARED, PLANNED -> "contrast";
    });
  }

  public static Span acquisition(AcquisitionStatus status) {
    return badge(status.name(), switch (status) {
      case DONE, ACCEPTED -> "success";
      case CANCELLED -> "error";
      case OFFERED, REVIEW, REQUESTED -> "contrast";
      case SKIPPED -> "";
    });
  }
}
