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

import com.svenruppert.publications.model.Arbeitszustand;
import com.svenruppert.publications.model.Veroeffentlichungsstatus;
import com.svenruppert.publications.model.Vertriebsstatus;
import com.vaadin.flow.component.html.Span;

/**
 * Kleine Sammlung wiederverwendbarer Lumo-Badges für die
 * Publikationsverwaltungs-Views. Bildet die Statuswerte der drei Dimensionen
 * auf die Lumo-Badge-Theme-Varianten ab (success/error/contrast), damit alle
 * Views (Themen-Arbeitsplatz, Redaktionstafel, Veröffentlichungssicht,
 * Verlauf) dieselbe Farbsprache verwenden.
 */
public final class PublicationUi {

  private PublicationUi() {
  }

  /** Ein Lumo-Badge mit optionaler Farbvariante ({@code "success"} …). */
  public static Span badge(String text, String variant) {
    Span span = new Span(text);
    span.getElement().getThemeList().add("badge");
    span.getElement().getThemeList().add("small");
    if (variant != null && !variant.isBlank()) {
      span.getElement().getThemeList().add(variant);
    }
    return span;
  }

  /** Neutrales Tag-Badge. */
  public static Span tag(String name) {
    return badge(name, "contrast");
  }

  public static Span arbeitszustand(Arbeitszustand zustand) {
    return badge(zustand.name(), switch (zustand) {
      case DONE -> "success";
      case CANCELLED -> "error";
      case IN_PROGRESS, IN_PLANUNG, REVIEW -> "contrast";
      case BACKLOG, SKIPPED -> "";
    });
  }

  public static Span herstellung(Veroeffentlichungsstatus status) {
    return badge(status.name(), switch (status) {
      case PUBLISHED -> "success";
      case UPDATE_NEEDED -> "error";
      case PREPARED, PLANNED -> "contrast";
    });
  }

  public static Span vertrieb(Vertriebsstatus status) {
    return badge(status.name(), switch (status) {
      case DONE, ACCEPTED -> "success";
      case CANCELLED -> "error";
      case OFFERED, REVIEW, REQUESTED -> "contrast";
      case SKIPPED -> "";
    });
  }
}
