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

import com.svenruppert.flow.i18n.I18nSupport;
import com.svenruppert.flow.security.model.AppUser;
import com.svenruppert.flow.security.roles.AuthorizationRole;
import com.svenruppert.flow.security.roles.VisibleFor;
import com.svenruppert.flow.views.MainLayout;
import com.svenruppert.flow.views.ui.PageHeader;
import com.svenruppert.flow.views.ui.TemplateBrand;
import com.svenruppert.jsentinel.authorization.api.SubjectStores;
import com.svenruppert.publications.model.Arbeitszustand;
import com.svenruppert.publications.model.Teil;
import com.svenruppert.publications.persistence.PublicationsProvider;
import com.svenruppert.publications.persistence.PublicationsRepository;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;

import java.util.List;

import com.vaadin.flow.router.Route;

/**
 * V3 — Redaktionstafel. Führt den redaktionellen Fortschritt aller {@link Teil}e
 * auf einen Blick: Spalten je {@link Arbeitszustand}, in denen die Teile als
 * Karten liegen. Das Fortschreiben des Zustands geschieht über den Zustands-
 * {@link Select} auf jeder Karte (die barrierearme, testbare Entsprechung des
 * Karten-Ziehens; Drag&amp;Drop ist eine spätere Ausbaustufe). Bediente Prozesse:
 * P0007, P0020 (redaktionelle Dimension).
 */
@Route(value = RedaktionstafelView.NAV, layout = MainLayout.class)
@VisibleFor(AuthorizationRole.USER)
public class RedaktionstafelView extends Composite<VerticalLayout> implements I18nSupport {

  public static final String NAV = "redaktion";

  private final transient PublicationsRepository repo = PublicationsProvider.repository();
  private final FlexLayout board = new FlexLayout();

  public RedaktionstafelView() {
    VerticalLayout root = getContent();
    root.setSizeFull();
    root.getStyle().set("gap", "var(--lumo-space-m)");

    root.add(new PageHeader(
        tr("tafel.heading", "Editorial board"),
        tr("tafel.subtitle", "Editorial state per part — change the state on a card.")));

    board.setWidthFull();
    board.getStyle().set("gap", "var(--lumo-space-m)");
    board.getStyle().set("overflow-x", "auto");
    board.setFlexWrap(FlexLayout.FlexWrap.NOWRAP);
    root.add(board);
    root.setFlexGrow(1, board);

    refresh();
  }

  private void refresh() {
    board.removeAll();
    List<Teil> alle = repo.issues().stream()
        .flatMap(i -> i.teile().stream())
        .toList();
    for (Arbeitszustand zustand : Arbeitszustand.values()) {
      List<Teil> inColumn = alle.stream()
          .filter(t -> t.arbeitszustand() == zustand)
          .toList();
      board.add(column(zustand, inColumn));
    }
  }

  private Div column(Arbeitszustand zustand, List<Teil> teile) {
    Div col = new Div();
    col.getStyle().set("flex", "0 0 240px");
    col.getStyle().set("background", "var(--lumo-contrast-5pct)");
    col.getStyle().set("border-radius", "var(--lumo-border-radius-l)");
    col.getStyle().set("padding", "var(--lumo-space-s)");
    col.getStyle().set("display", "flex");
    col.getStyle().set("flex-direction", "column");
    col.getStyle().set("gap", "var(--lumo-space-s)");

    Div head = new Div(PublicationUi.arbeitszustand(zustand),
        new Span(" " + teile.size()));
    head.getStyle().set("font-weight", "600");
    col.add(head);

    for (Teil teil : teile) {
      col.add(card(teil));
    }
    return col;
  }

  private Div card(Teil teil) {
    Div card = new Div();
    card.addClassName(TemplateBrand.CSS_CARD);
    card.getStyle().set("padding", "var(--lumo-space-s)");

    String prefix = teil.issue() != null ? teil.issue().titel() : "";
    Span title = new Span(prefix);
    title.getStyle().set("font-weight", "600");
    title.getStyle().set("display", "block");
    Span part = new Span(tr("tafel.part", "Part {0}", teil.reihenfolge())
        + " · " + teil.sprachfassungen().size() + " "
        + tr("tafel.versions", "versions"));
    part.getStyle().set("color", "var(--lumo-secondary-text-color)");
    part.getStyle().set("font-size", "var(--lumo-font-size-s)");
    part.getStyle().set("display", "block");

    Select<Arbeitszustand> move = new Select<>();
    move.setItems(Arbeitszustand.values());
    move.setValue(teil.arbeitszustand());
    move.setWidthFull();
    move.addValueChangeListener(e -> {
      if (e.getValue() != null && e.getValue() != teil.arbeitszustand()) {
        teil.wechsleZustand(e.getValue(), akteur());
        repo.persist();
        refresh();
      }
    });

    card.add(title, part, move);
    return card;
  }

  private static String akteur() {
    return SubjectStores.subjectStore().currentSubject(AppUser.class)
        .map(AppUser::name).orElse("system");
  }
}
