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
import com.svenruppert.flow.views.ui.EmptyState;
import com.svenruppert.flow.views.ui.FilterBar;
import com.svenruppert.flow.views.ui.PageHeader;
import com.svenruppert.publications.model.Issue;
import com.svenruppert.publications.model.Tag;
import com.svenruppert.publications.model.Teil;
import com.svenruppert.publications.persistence.PublicationsProvider;
import com.svenruppert.publications.persistence.PublicationsRepository;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * V1 — Themen-Arbeitsplatz. Einstieg in den Bestand: ein Master-{@link Grid}
 * der {@link Issue}s links (mit {@link FilterBar} nach Titel und Tags), rechts
 * die Detailsicht des gewählten Themas mit Tags und der geordneten Folge seiner
 * {@link Teil}e. Von hier „Öffnen" in den Sprachfassungs-Editor (V2) und
 * „Verlauf" in die Verlaufsansicht (V5).
 *
 * <p>Bediente Prozesse: P0001 (Thema erfassen), P0002 (Tags), P0003 (Teile
 * gliedern), P0019 (durchsuchen).
 */
@Route(value = ThemenView.NAV, layout = MainLayout.class)
@VisibleFor(AuthorizationRole.USER)
public class ThemenView extends Composite<VerticalLayout> implements I18nSupport {

  public static final String NAV = "themen";

  private final transient PublicationsRepository repo = PublicationsProvider.repository();

  private final Grid<Issue> grid = new Grid<>(Issue.class, false);
  private final FilterBar filterBar = new FilterBar();
  private final TextField suche;
  private final MultiSelectComboBox<Tag> tagFilter;
  private final Div detail = new Div();

  {
    suche = filterBar.addText(tr("themen.filter.search", "Search"),
        tr("themen.filter.search.ph", "Title contains…"));
    tagFilter = filterBar.addMultiSelect(tr("themen.filter.tags", "Tags"),
        allTags(), tr("themen.filter.tags.ph", "Any tag"));
    tagFilter.setItemLabelGenerator(Tag::name);
  }

  public ThemenView() {
    VerticalLayout root = getContent();
    root.setSizeFull();
    root.getStyle().set("gap", "var(--lumo-space-m)");

    root.add(new PageHeader(
        tr("themen.heading", "Topic workspace"),
        tr("themen.subtitle", "Issues, their parts and tags — the entry into the backlog."))
        .withActions(primary(tr("themen.new", "+ Topic"), e -> openCreateIssueDialog())));

    suche.addValueChangeListener(e -> refreshMaster());
    tagFilter.addValueChangeListener(e -> refreshMaster());
    filterBar.onClear(this::refreshMaster);

    grid.addColumn(Issue::titel).setHeader(tr("themen.col.title", "Title")).setFlexGrow(1);
    grid.addColumn(i -> i.teile().size()).setHeader(tr("themen.col.parts", "Parts")).setAutoWidth(true);
    grid.addComponentColumn(this::statusSummary).setHeader(tr("themen.col.status", "Status")).setAutoWidth(true);
    grid.setSizeFull();
    grid.asSingleSelect().addValueChangeListener(e -> showDetail(e.getValue()));

    VerticalLayout master = new VerticalLayout(filterBar, grid);
    master.setPadding(false);
    master.setSpacing(false);
    master.setSizeFull();
    master.setFlexGrow(1, grid);

    detail.getStyle().set("padding", "var(--lumo-space-m)");
    detail.setSizeFull();

    SplitLayout split = new SplitLayout(master, detail);
    split.setSizeFull();
    split.setSplitterPosition(46);
    root.add(split);
    root.setFlexGrow(1, split);

    refreshMaster();
    showDetail(null);
  }

  // ── master ───────────────────────────────────────────────────────────────

  private void refreshMaster() {
    String needle = suche.getValue() == null ? "" : suche.getValue().strip().toLowerCase();
    Set<Tag> wanted = tagFilter.getValue();
    List<Issue> issues = repo.issues().stream()
        .filter(i -> needle.isEmpty() || i.titel().toLowerCase().contains(needle))
        .filter(i -> wanted.isEmpty() || i.tags().stream().anyMatch(wanted::contains))
        .sorted((a, b) -> a.titel().compareToIgnoreCase(b.titel()))
        .toList();
    grid.setItems(issues);
    filterBar.setCount(issues.size(), tr("themen.unit", "topics"));
  }

  private HorizontalLayout statusSummary(Issue issue) {
    HorizontalLayout row = new HorizontalLayout();
    row.setSpacing(false);
    row.getStyle().set("gap", "var(--lumo-space-xs)");
    issue.teileInReihenfolge().stream()
        .map(Teil::arbeitszustand)
        .distinct()
        .forEach(z -> row.add(PublicationUi.arbeitszustand(z)));
    return row;
  }

  // ── detail ─────────────────────────────────────────────────────────────

  private void showDetail(Issue issue) {
    detail.removeAll();
    if (issue == null) {
      detail.add(new EmptyState(VaadinIcon.GLOBE,
          tr("themen.detail.empty.title", "No topic selected"),
          tr("themen.detail.empty.body", "Pick a topic on the left, or create one.")));
      return;
    }

    H3 title = new H3(issue.titel());
    detail.add(title);
    if (issue.herkunft() != null) {
      Span herkunft = new Span(tr("themen.detail.origin", "Origin: {0}", issue.herkunft()));
      herkunft.getStyle().set("color", "var(--lumo-secondary-text-color)");
      herkunft.getStyle().set("font-size", "var(--lumo-font-size-s)");
      detail.add(herkunft, new Div());
    }

    FlexLayout tags = new FlexLayout();
    tags.setFlexWrap(FlexLayout.FlexWrap.WRAP);
    tags.getStyle().set("gap", "var(--lumo-space-xs)");
    issue.tags().forEach(t -> tags.add(PublicationUi.tag(t.name())));
    Button addTag = new Button(tr("themen.tag.add", "+ Tag"), e -> openAddTagDialog(issue));
    addTag.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
    tags.add(addTag);
    detail.add(tags);

    Grid<Teil> teile = new Grid<>(Teil.class, false);
    teile.addColumn(t -> "#" + t.reihenfolge()).setHeader("#").setAutoWidth(true);
    teile.addColumn(t -> tr("themen.part", "Part {0}", t.reihenfolge())).setHeader(tr("themen.col.part", "Part")).setFlexGrow(1);
    teile.addColumn(t -> t.sprachfassungen().size()).setHeader(tr("themen.col.versions", "Versions")).setAutoWidth(true);
    teile.addComponentColumn(t -> PublicationUi.arbeitszustand(t.arbeitszustand()))
        .setHeader(tr("themen.col.state", "State")).setAutoWidth(true);
    teile.addComponentColumn(this::teilActions).setAutoWidth(true);
    teile.setItems(issue.teileInReihenfolge());
    teile.setAllRowsVisible(true);
    detail.add(teile);

    Button addTeil = new Button(tr("themen.part.add", "+ Add part"), e -> {
      issue.addTeil();
      repo.persist();
      showDetail(issue);
      refreshMaster();
    });
    addTeil.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    detail.add(addTeil);
  }

  private HorizontalLayout teilActions(Teil teil) {
    Button open = new Button(tr("themen.open", "Open"),
        e -> navigate("teil/" + teil.id()));
    open.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
    Button verlauf = new Button(tr("themen.history", "History"),
        e -> navigate("verlauf/teil/" + teil.id()));
    verlauf.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
    return new HorizontalLayout(open, verlauf);
  }

  // ── dialogs ──────────────────────────────────────────────────────────────

  private void openCreateIssueDialog() {
    Dialog dialog = new Dialog();
    dialog.setHeaderTitle(tr("themen.new.title", "New topic"));
    TextField titel = new TextField(tr("themen.new.field", "Title"));
    titel.setPlaceholder("Blog – Area – Topic");
    titel.setWidthFull();
    Button save = primary(tr("common.save", "Save"), e -> {
      String value = titel.getValue() == null ? "" : titel.getValue().strip();
      if (value.isEmpty()) {
        titel.setInvalid(true);
        titel.setErrorMessage(tr("themen.new.required", "Title required"));
        return;
      }
      repo.neuesIssue(value);
      dialog.close();
      refreshMaster();
    });
    Button cancel = new Button(tr("common.cancel", "Cancel"), e -> dialog.close());
    dialog.add(titel);
    dialog.getFooter().add(cancel, save);
    dialog.open();
    titel.focus();
  }

  private void openAddTagDialog(Issue issue) {
    Dialog dialog = new Dialog();
    dialog.setHeaderTitle(tr("themen.tag.title", "Add tag"));
    TextField name = new TextField(tr("themen.tag.field", "Tag"));
    name.setWidthFull();
    Button save = primary(tr("common.save", "Save"), e -> {
      String value = name.getValue() == null ? "" : name.getValue().strip();
      if (value.isEmpty()) {
        name.setInvalid(true);
        return;
      }
      issue.addTag(new Tag(value));
      repo.persist();
      dialog.close();
      showDetail(issue);
      tagFilter.setItems(allTags());
    });
    dialog.add(name);
    dialog.getFooter().add(new Button(tr("common.cancel", "Cancel"), e -> dialog.close()), save);
    dialog.open();
    name.focus();
  }

  // ── helpers ────────────────────────────────────────────────────────────

  private List<Tag> allTags() {
    return repo.issues().stream()
        .flatMap(i -> i.tags().stream())
        .collect(Collectors.toCollection(() -> new TreeSet<>(
            (a, b) -> a.name().compareToIgnoreCase(b.name()))))
        .stream().toList();
  }

  private static Button primary(String label, com.vaadin.flow.component.ComponentEventListener<com.vaadin.flow.component.ClickEvent<Button>> listener) {
    Button b = new Button(label, listener);
    b.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    return b;
  }

  private void navigate(String path) {
    UI.getCurrent().navigate(path);
  }
}
