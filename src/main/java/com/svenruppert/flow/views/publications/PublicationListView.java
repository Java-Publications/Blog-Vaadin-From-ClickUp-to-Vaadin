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
import com.svenruppert.publications.model.AcquisitionStatus;
import com.svenruppert.publications.model.Language;
import com.svenruppert.publications.model.Publication;
import com.svenruppert.publications.model.PublicationPlace;
import com.svenruppert.publications.model.ProductionStatus;
import com.svenruppert.publications.persistence.PublicationsProvider;
import com.svenruppert.publications.persistence.PublicationsRepository;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Overview of all {@link Publication}s (drawer entry point) with a filter over
 * the production status; "Open" leads into the two-column {@link PublicationView}
 * (V4).
 */
@Route(value = PublicationListView.NAV, layout = MainLayout.class)
@VisibleFor(AuthorizationRole.USER)
public class PublicationListView extends Composite<VerticalLayout>
    implements I18nSupport {

  public static final String NAV = "publications";

  private static final long serialVersionUID = 1L;

  private final transient PublicationsRepository repo = PublicationsProvider.repository();
  private final Grid<Publication> grid = new Grid<>(Publication.class, false);
  private final FilterBar filterBar = new FilterBar();

  // Complex, multi-criteria search over the publications (U).
  private final TextField search;
  private final MultiSelectComboBox<Language> languageFilter;
  private final MultiSelectComboBox<PublicationPlace> placeFilter;
  private final ComboBox<AcquisitionStatus> acquisitionFilter;
  private final ComboBox<ProductionStatus> statusFilter;
  private final DatePicker since;
  private final DatePicker until;

  {
    search = filterBar.addText(tr("liste.filter.search", "Search"),
        tr("liste.filter.search.ph", "Blog post, place, language, client…"));
    languageFilter = filterBar.addMultiSelect(tr("liste.filter.lang", "Language"),
        List.of(Language.values()), tr("liste.filter.lang.ph", "Any language"));
    languageFilter.setItemLabelGenerator(Language::name);
    placeFilter = filterBar.addMultiSelect(tr("liste.filter.place", "Place"),
        repo.publicationPlaces(), tr("liste.filter.place.ph", "Any place"));
    placeFilter.setItemLabelGenerator(PublicationPlace::name);
    acquisitionFilter = filterBar.addSingleSelect(tr("liste.filter.acquisition", "Acquisition"),
        AcquisitionStatus.values(), tr("liste.filter.acquisition.ph", "Any status"));
    statusFilter = filterBar.addSingleSelect(tr("liste.filter.status", "Production"),
        ProductionStatus.values(), tr("liste.filter.status.ph", "Any status"));
    since = filterBar.addDate(tr("liste.filter.since", "Date from"),
        tr("liste.filter.since.ph", "from"));
    until = filterBar.addDate(tr("liste.filter.until", "Date to"),
        tr("liste.filter.until.ph", "to"));
  }

  public PublicationListView() {
    VerticalLayout root = getContent();
    root.setSizeFull();
    root.getStyle().set("gap", "var(--lumo-space-m)");

    root.add(new PageHeader(
        tr("liste.heading", "Publications"),
        tr("liste.subtitle", "Every publication across all versions and places.")));

    search.addValueChangeListener(e -> refresh());
    languageFilter.addValueChangeListener(e -> refresh());
    placeFilter.addValueChangeListener(e -> refresh());
    acquisitionFilter.addValueChangeListener(e -> refresh());
    statusFilter.addValueChangeListener(e -> refresh());
    since.addValueChangeListener(e -> refresh());
    until.addValueChangeListener(e -> refresh());
    filterBar.onClear(this::refresh);
    root.add(filterBar);

    // The blog-post column carries the long text and takes the remaining width;
    // the others are narrow and fixed. Every column is user-resizable.
    grid.addColumn(v -> PublicationUi.blogPost(repo.partOf(v.version()).orElse(null)))
        .setHeader(tr("liste.col.blogpost", "Blog post")).setFlexGrow(1).setResizable(true);
    grid.addColumn(v -> v.version().language().name())
        .setHeader(tr("liste.col.lang", "Language")).setWidth("8em").setFlexGrow(0).setResizable(true);
    grid.addColumn(v -> v.place().name())
        .setHeader(tr("liste.col.place", "Place")).setWidth("12em").setFlexGrow(0).setResizable(true);
    grid.addComponentColumn(v -> PublicationUi.acquisition(v.acquisitionStatus()))
        .setHeader(tr("liste.col.acquisition", "Acquisition")).setWidth("9em").setFlexGrow(0).setResizable(true);
    grid.addComponentColumn(v -> PublicationUi.production(v.productionStatus()))
        .setHeader(tr("liste.col.production", "Production")).setWidth("9em").setFlexGrow(0).setResizable(true);
    grid.addColumn(v -> v.date() == null ? "" : v.date().toString())
        .setHeader(tr("liste.col.date", "Date")).setWidth("8em").setFlexGrow(0).setResizable(true);
    grid.addComponentColumn(v -> {
      Button open = new Button(tr("liste.open", "Open"),
          e -> UI.getCurrent().navigate(PublicationView.NAV + "/" + v.id()));
      open.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
      return open;
    }).setHeader("").setWidth("6em").setFlexGrow(0);
    grid.setSizeFull();
    root.add(grid);
    root.setFlexGrow(1, grid);

    refresh();
  }

  private void refresh() {
    String needle = search.getValue() == null ? "" : search.getValue().strip().toLowerCase();
    Set<Language> langs = languageFilter.getValue();
    Set<PublicationPlace> places = placeFilter.getValue();
    AcquisitionStatus acq = acquisitionFilter.getValue();
    ProductionStatus prod = statusFilter.getValue();
    LocalDate from = since.getValue();
    LocalDate to = until.getValue();

    List<Publication> items = repo.allPublications().stream()
        .filter(v -> needle.isEmpty() || searchable(v).contains(needle))
        .filter(v -> langs.isEmpty() || langs.contains(v.version().language()))
        .filter(v -> places.isEmpty() || places.contains(v.place()))
        .filter(v -> acq == null || v.acquisitionStatus() == acq)
        .filter(v -> prod == null || v.productionStatus() == prod)
        .filter(v -> from == null || (v.date() != null && !v.date().isBefore(from)))
        .filter(v -> to == null || (v.date() != null && !v.date().isAfter(to)))
        .toList();
    grid.setItems(items);
    filterBar.setCount(items.size(), tr("liste.unit", "publications"));
  }

  /** The lower-cased text a free-text search matches against for one publication. */
  private String searchable(Publication v) {
    String post = PublicationUi.blogPost(repo.partOf(v.version()).orElse(null));
    return (post + " " + v.place().name() + " " + v.version().language().name() + " "
        + (v.client() == null ? "" : v.client()) + " "
        + (v.link() == null ? "" : v.link())).toLowerCase();
  }
}
