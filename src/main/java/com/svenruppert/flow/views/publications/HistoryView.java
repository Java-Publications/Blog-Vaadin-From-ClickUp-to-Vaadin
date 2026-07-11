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
import com.svenruppert.flow.security.roles.AuthorizationRole;
import com.svenruppert.flow.security.roles.VisibleFor;
import com.svenruppert.flow.views.MainLayout;
import com.svenruppert.flow.views.ui.BackButton;
import com.svenruppert.flow.views.ui.EmptyState;
import com.svenruppert.flow.views.ui.PageHeader;
import com.svenruppert.publications.model.StatusChange;
import com.svenruppert.publications.model.StatusHistory;
import com.svenruppert.publications.persistence.PublicationsProvider;
import com.svenruppert.publications.persistence.PublicationsRepository;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.WildcardParameter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * V5 — History view. Shows one of the three status chains completely, ordered by
 * the sequence number — as an audit trail and a visible proof of orthogonality.
 * Read-only. Reachable via {@code verlauf/teil/<id>},
 * {@code verlauf/akquise/<id>}, {@code verlauf/herstellung/<id>} or without a path
 * as a selection hint. Served process: P0015.
 */
@Route(value = HistoryView.NAV, layout = MainLayout.class)
@VisibleFor(AuthorizationRole.USER)
public class HistoryView extends Composite<VerticalLayout>
    implements HasUrlParameter<String>, I18nSupport {

  public static final String NAV = "verlauf";

  private static final long serialVersionUID = 1L;

  private final transient PublicationsRepository repo = PublicationsProvider.repository();
  private final Div body = new Div();

  public HistoryView() {
    VerticalLayout root = getContent();
    root.setSizeFull();
    root.getStyle().set("gap", "var(--lumo-space-m)");
    body.setSizeFull();
    root.add(body);
  }

  @Override
  public void setParameter(BeforeEvent event, @WildcardParameter String path) {
    body.removeAll();
    String[] parts = (path == null ? "" : path).split("/");
    if (parts.length < 2) {
      showEmpty();
      return;
    }
    String kind = parts[0];
    Optional<UUID> id = parse(parts[1]);
    if (id.isEmpty()) {
      showEmpty();
      return;
    }
    // Back to where the history was opened from (S): a part or a publication.
    String backRoute = switch (kind) {
      case "teil" -> "teil/" + parts[1];
      case "akquise", "herstellung" -> "veroeffentlichung/" + parts[1];
      default -> null;
    };
    if (backRoute != null) {
      body.add(BackButton.to(tr("verlauf.back", "Back"), backRoute));
    }
    switch (kind) {
      case "teil" -> repo.findPart(id.get())
          .ifPresentOrElse(p -> show(tr("verlauf.dim.editorial", "Editorial state"), p.editorialWork()),
              this::showEmpty);
      case "akquise" -> repo.findPublication(id.get())
          .ifPresentOrElse(v -> show(tr("verlauf.dim.acquisition", "Acquisition"), v.acquisition()),
              this::showEmpty);
      case "herstellung" -> repo.findPublication(id.get())
          .ifPresentOrElse(v -> show(tr("verlauf.dim.production", "Production"), v.production()),
              this::showEmpty);
      default -> showEmpty();
    }
  }

  private static Optional<UUID> parse(String id) {
    try {
      return Optional.of(UUID.fromString(id));
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }

  private void showEmpty() {
    body.add(new EmptyState(VaadinIcon.TIMER,
        tr("verlauf.empty.title", "No history selected"),
        tr("verlauf.empty.body", "Open a history from a part or a publication.")));
  }

  private void show(String dimension, StatusHistory<?> history) {
    body.add(new PageHeader(
        tr("verlauf.heading", "History — {0}", dimension),
        tr("verlauf.subtitle", "Ordered by sequence number, not by timestamp.")));

    List<StatusChange<?>> rows = new ArrayList<>(history.events());
    Grid<StatusChange<?>> grid = new Grid<>();
    grid.addColumn(StatusChange::sequence).setHeader(tr("verlauf.col.seq", "Seq")).setAutoWidth(true);
    grid.addColumn(c -> String.valueOf(c.from())).setHeader(tr("verlauf.col.from", "From")).setAutoWidth(true);
    grid.addColumn(c -> String.valueOf(c.to())).setHeader(tr("verlauf.col.to", "To")).setAutoWidth(true);
    grid.addColumn(c -> c.actor() == null ? "—" : c.actor()).setHeader(tr("verlauf.col.actor", "Actor")).setFlexGrow(1);
    grid.addColumn(c -> c.timestamp().toString()).setHeader(tr("verlauf.col.time", "Timestamp")).setAutoWidth(true);
    grid.setItems(rows);
    grid.setAllRowsVisible(true);
    body.add(grid);

    Paragraph note = new Paragraph(tr("verlauf.note",
        "The order rests on the strictly ascending sequence number, not on the "
            + "timestamp. The history is append-only; the current state is the "
            + "target of the last event."));
    note.getStyle().set("color", "var(--lumo-secondary-text-color)");
    note.getStyle().set("font-size", "var(--lumo-font-size-s)");
    body.add(note);
  }
}
