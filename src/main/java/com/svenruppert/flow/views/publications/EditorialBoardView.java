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
import com.svenruppert.publications.model.EditorialState;
import com.svenruppert.publications.model.Part;
import com.svenruppert.publications.persistence.PublicationsProvider;
import com.svenruppert.publications.persistence.PublicationsRepository;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.Route;

import java.util.List;

/**
 * V3 — Editorial board. Shows the editorial progress of all {@link Part}s at a
 * glance: one column per {@link EditorialState}, in which the parts sit as cards.
 * Advancing the state happens through the state {@link Select} on each card (the
 * low-barrier, testable equivalent of dragging a card; drag&drop is a later
 * enhancement). Served processes: P0007, P0020 (editorial dimension).
 */
@Route(value = EditorialBoardView.NAV, layout = MainLayout.class)
@VisibleFor(AuthorizationRole.USER)
public class EditorialBoardView extends Composite<VerticalLayout> implements I18nSupport {

  public static final String NAV = "redaktion";

  private final transient PublicationsRepository repo = PublicationsProvider.repository();
  private final FlexLayout board = new FlexLayout();

  public EditorialBoardView() {
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
    List<Part> all = repo.issues().stream()
        .flatMap(i -> i.parts().stream())
        .toList();
    for (EditorialState state : EditorialState.values()) {
      List<Part> inColumn = all.stream()
          .filter(p -> p.editorialState() == state)
          .toList();
      board.add(column(state, inColumn));
    }
  }

  private Div column(EditorialState state, List<Part> parts) {
    Div col = new Div();
    col.getStyle().set("flex", "0 0 240px");
    col.getStyle().set("background", "var(--lumo-contrast-5pct)");
    col.getStyle().set("border-radius", "var(--lumo-border-radius-l)");
    col.getStyle().set("padding", "var(--lumo-space-s)");
    col.getStyle().set("display", "flex");
    col.getStyle().set("flex-direction", "column");
    col.getStyle().set("gap", "var(--lumo-space-s)");

    Div head = new Div(PublicationUi.editorialState(state), new Span(" " + parts.size()));
    head.getStyle().set("font-weight", "600");
    col.add(head);

    for (Part part : parts) {
      col.add(card(part));
    }
    return col;
  }

  private Div card(Part part) {
    Div card = new Div();
    card.addClassName(TemplateBrand.CSS_CARD);
    card.getStyle().set("padding", "var(--lumo-space-s)");

    String prefix = part.issue() != null ? part.issue().title() : "";
    Span title = new Span(prefix);
    title.getStyle().set("font-weight", "600");
    title.getStyle().set("display", "block");
    Span info = new Span(tr("tafel.part", "Part {0}", part.position())
        + " · " + part.languageVersions().size() + " " + tr("tafel.versions", "versions"));
    info.getStyle().set("color", "var(--lumo-secondary-text-color)");
    info.getStyle().set("font-size", "var(--lumo-font-size-s)");
    info.getStyle().set("display", "block");

    Select<EditorialState> move = new Select<>();
    move.setItems(EditorialState.values());
    move.setValue(part.editorialState());
    move.setWidthFull();
    move.addValueChangeListener(e -> {
      if (e.getValue() != null && e.getValue() != part.editorialState()) {
        part.changeState(e.getValue(), actor());
        repo.persist();
        refresh();
      }
    });

    card.add(title, info, move);
    return card;
  }

  private static String actor() {
    return SubjectStores.subjectStore().currentSubject(AppUser.class)
        .map(AppUser::name).orElse("system");
  }
}
