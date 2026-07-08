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
import com.svenruppert.flow.views.ui.FilterBar;
import com.svenruppert.flow.views.ui.PageHeader;
import com.svenruppert.publications.model.Veroeffentlichung;
import com.svenruppert.publications.model.Veroeffentlichungsstatus;
import com.svenruppert.publications.persistence.PublicationsProvider;
import com.svenruppert.publications.persistence.PublicationsRepository;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import java.util.List;

/**
 * Übersicht aller {@link Veroeffentlichung}en (Einstieg aus dem Drawer) mit
 * einem Filter über den Herstellungsstatus; „Öffnen" führt in die
 * zweispaltige {@link VeroeffentlichungView} (V4).
 */
@Route(value = VeroeffentlichungslisteView.NAV, layout = MainLayout.class)
@VisibleFor(AuthorizationRole.USER)
public class VeroeffentlichungslisteView extends Composite<VerticalLayout>
    implements I18nSupport {

  public static final String NAV = "veroeffentlichungen";

  private final transient PublicationsRepository repo = PublicationsProvider.repository();
  private final Grid<Veroeffentlichung> grid = new Grid<>(Veroeffentlichung.class, false);
  private final FilterBar filterBar = new FilterBar();
  private final ComboBox<Veroeffentlichungsstatus> statusFilter;

  {
    statusFilter = filterBar.addSingleSelect(tr("liste.filter.status", "Production"),
        Veroeffentlichungsstatus.values(), tr("liste.filter.status.ph", "Any status"));
  }

  public VeroeffentlichungslisteView() {
    VerticalLayout root = getContent();
    root.setSizeFull();
    root.getStyle().set("gap", "var(--lumo-space-m)");

    root.add(new PageHeader(
        tr("liste.heading", "Publications"),
        tr("liste.subtitle", "Every publication across all versions and places.")));

    statusFilter.addValueChangeListener(e -> refresh());
    filterBar.onClear(this::refresh);
    root.add(filterBar);

    grid.addColumn(v -> v.fassung().sprache().name()).setHeader(tr("liste.col.lang", "Language")).setAutoWidth(true);
    grid.addColumn(v -> v.ort().name()).setHeader(tr("liste.col.place", "Place")).setFlexGrow(1);
    grid.addComponentColumn(v -> PublicationUi.vertrieb(v.akquisestatus()))
        .setHeader(tr("liste.col.acquisition", "Acquisition")).setAutoWidth(true);
    grid.addComponentColumn(v -> PublicationUi.herstellung(v.herstellungsstatus()))
        .setHeader(tr("liste.col.production", "Production")).setAutoWidth(true);
    grid.addColumn(v -> v.datum() == null ? "" : v.datum().toString())
        .setHeader(tr("liste.col.date", "Date")).setAutoWidth(true);
    grid.addComponentColumn(v -> {
      Button open = new Button(tr("liste.open", "Open"),
          e -> UI.getCurrent().navigate(VeroeffentlichungView.NAV + "/" + v.id()));
      open.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
      return open;
    }).setAutoWidth(true);
    grid.setSizeFull();
    root.add(grid);
    root.setFlexGrow(1, grid);

    refresh();
  }

  private void refresh() {
    Veroeffentlichungsstatus wanted = statusFilter.getValue();
    List<Veroeffentlichung> items = repo.alleVeroeffentlichungen().stream()
        .filter(v -> wanted == null || v.herstellungsstatus() == wanted)
        .toList();
    grid.setItems(items);
    filterBar.setCount(items.size(), tr("liste.unit", "publications"));
  }
}
