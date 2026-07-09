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
import com.svenruppert.publications.model.Part;
import com.svenruppert.publications.model.Tag;
import com.svenruppert.publications.persistence.PublicationsProvider;
import com.svenruppert.publications.persistence.PublicationsRepository;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
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
 * V1 — Topics workspace. The entry into the backlog: a master {@link Grid} of the
 * {@link Issue}s on the left (with a {@link FilterBar} by title and tags), and on
 * the right the detail view of the selected topic with its tags and the ordered
 * sequence of its {@link Part}s. From here "Open" leads into the language-version
 * editor (V2) and "History" into the history view (V5).
 *
 * <p>Served processes: P0001 (create topic), P0002 (tags), P0003 (split into
 * parts), P0019 (search).
 */
@Route(value = TopicsView.NAV, layout = MainLayout.class)
@VisibleFor(AuthorizationRole.USER)
public class TopicsView extends Composite<VerticalLayout> implements I18nSupport {

  public static final String NAV = "themen";

  private final transient PublicationsRepository repo = PublicationsProvider.repository();

  private final Grid<Issue> grid = new Grid<>(Issue.class, false);
  private final FilterBar filterBar = new FilterBar();
  private final TextField searchField;
  private final MultiSelectComboBox<Tag> tagFilter;
  private final Div detail = new Div();

  {
    searchField = filterBar.addText(tr("themen.filter.search", "Search"),
        tr("themen.filter.search.ph", "Title contains…"));
    tagFilter = filterBar.addMultiSelect(tr("themen.filter.tags", "Tags"),
        allTags(), tr("themen.filter.tags.ph", "Any tag"));
    tagFilter.setItemLabelGenerator(Tag::name);
  }

  public TopicsView() {
    VerticalLayout root = getContent();
    root.setSizeFull();
    root.getStyle().set("gap", "var(--lumo-space-m)");

    root.add(new PageHeader(
        tr("themen.heading", "Topic workspace"),
        tr("themen.subtitle", "Issues, their parts and tags — the entry into the backlog."))
        .withActions(primary(tr("themen.new", "+ Topic"), e -> openCreateIssueDialog())));

    searchField.addValueChangeListener(e -> refreshMaster());
    tagFilter.addValueChangeListener(e -> refreshMaster());
    filterBar.onClear(this::refreshMaster);

    grid.addColumn(Issue::title).setHeader(tr("themen.col.title", "Title")).setFlexGrow(1);
    grid.addColumn(i -> i.parts().size()).setHeader(tr("themen.col.parts", "Parts")).setAutoWidth(true);
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
    String needle = searchField.getValue() == null ? "" : searchField.getValue().strip().toLowerCase();
    Set<Tag> wanted = tagFilter.getValue();
    List<Issue> issues = repo.issues().stream()
        .filter(i -> needle.isEmpty() || i.title().toLowerCase().contains(needle))
        .filter(i -> wanted.isEmpty() || i.tags().stream().anyMatch(wanted::contains))
        .sorted((a, b) -> a.title().compareToIgnoreCase(b.title()))
        .toList();
    grid.setItems(issues);
    filterBar.setCount(issues.size(), tr("themen.unit", "topics"));
  }

  private HorizontalLayout statusSummary(Issue issue) {
    HorizontalLayout row = new HorizontalLayout();
    row.setSpacing(false);
    row.getStyle().set("gap", "var(--lumo-space-xs)");
    issue.partsInOrder().stream()
        .map(Part::editorialState)
        .distinct()
        .forEach(s -> row.add(PublicationUi.editorialState(s)));
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

    H3 title = new H3(issue.title());
    detail.add(title);
    if (issue.origin() != null) {
      Span origin = new Span(tr("themen.detail.origin", "Origin: {0}", issue.origin()));
      origin.getStyle().set("color", "var(--lumo-secondary-text-color)");
      origin.getStyle().set("font-size", "var(--lumo-font-size-s)");
      detail.add(origin, new Div());
    }

    FlexLayout tags = new FlexLayout();
    tags.setFlexWrap(FlexLayout.FlexWrap.WRAP);
    tags.getStyle().set("gap", "var(--lumo-space-xs)");
    issue.tags().forEach(t -> tags.add(removableTag(issue, t)));
    Button addTag = new Button(tr("themen.tag.add", "+ Tag"), e -> openAddTagDialog(issue));
    addTag.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
    tags.add(addTag);
    detail.add(tags);

    Grid<Part> parts = new Grid<>(Part.class, false);
    parts.addColumn(p -> "#" + p.position()).setHeader("#").setAutoWidth(true);
    parts.addColumn(p -> tr("themen.part", "Part {0}", p.position())).setHeader(tr("themen.col.part", "Part")).setFlexGrow(1);
    parts.addColumn(p -> p.languageVersions().size()).setHeader(tr("themen.col.versions", "Versions")).setAutoWidth(true);
    parts.addComponentColumn(p -> PublicationUi.editorialState(p.editorialState()))
        .setHeader(tr("themen.col.state", "State")).setAutoWidth(true);
    parts.addComponentColumn(this::partActions).setAutoWidth(true);
    parts.setItems(issue.partsInOrder());
    parts.setAllRowsVisible(true);
    detail.add(parts);

    Button addPart = new Button(tr("themen.part.add", "+ Add part"), e -> {
      issue.addPart();
      repo.persist();
      showDetail(issue);
      refreshMaster();
    });
    addPart.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    detail.add(addPart);
  }

  private HorizontalLayout partActions(Part part) {
    Button open = new Button(tr("themen.open", "Open"),
        e -> navigate("teil/" + part.id()));
    open.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
    Button history = new Button(tr("themen.history", "History"),
        e -> navigate("verlauf/teil/" + part.id()));
    history.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
    return new HorizontalLayout(open, history);
  }

  /** A tag badge carrying a ✕ that removes the tag (P0002). */
  private Span removableTag(Issue issue, Tag tag) {
    Span badge = PublicationUi.tag(tag.name());
    Button remove = new Button(VaadinIcon.CLOSE_SMALL.create(), e -> {
      issue.removeTag(tag);
      repo.persist();
      showDetail(issue);
      tagFilter.setItems(allTags());
      refreshMaster();
    });
    remove.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
    remove.getElement().setAttribute("aria-label",
        tr("themen.tag.remove", "Remove tag {0}", tag.name()));
    badge.add(remove);
    return badge;
  }

  // ── dialogs ──────────────────────────────────────────────────────────────

  private void openCreateIssueDialog() {
    Dialog dialog = new Dialog();
    dialog.setHeaderTitle(tr("themen.new.title", "New topic"));
    TextField title = new TextField(tr("themen.new.field", "Title"));
    title.setPlaceholder("Blog – Area – Topic");
    title.setWidthFull();
    Button save = primary(tr("common.save", "Save"), e -> {
      String value = title.getValue() == null ? "" : title.getValue().strip();
      if (value.isEmpty()) {
        title.setInvalid(true);
        title.setErrorMessage(tr("themen.new.required", "Title required"));
        return;
      }
      repo.createIssue(value);
      dialog.close();
      refreshMaster();
    });
    Button cancel = new Button(tr("common.cancel", "Cancel"), e -> dialog.close());
    dialog.add(title);
    dialog.getFooter().add(cancel, save);
    dialog.open();
    title.focus();
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

  private static Button primary(String label, ComponentEventListener<ClickEvent<Button>> listener) {
    Button b = new Button(label, listener);
    b.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    return b;
  }

  private void navigate(String path) {
    UI.getCurrent().navigate(path);
  }
}
