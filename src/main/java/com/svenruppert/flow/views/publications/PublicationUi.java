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

import com.svenruppert.publications.model.AcquisitionStatus;
import com.svenruppert.publications.model.EditorialState;
import com.svenruppert.publications.model.ProductionStatus;
import com.vaadin.flow.component.html.Span;

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
