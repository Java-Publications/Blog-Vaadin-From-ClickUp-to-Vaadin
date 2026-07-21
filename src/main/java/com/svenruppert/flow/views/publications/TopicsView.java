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
import com.svenruppert.publications.model.EditorialState;
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
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.dnd.GridDropLocation;
import com.vaadin.flow.component.grid.dnd.GridDropMode;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.markdown.Markdown;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
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

  public static final String NAV = "topics";

  private static final long serialVersionUID = 1L;

  /**
   * A bare {@code http(s)} URL that is not already the target of a markdown link
   * (not preceded by {@code (}, {@code <} or {@code ]}) and does not end on
   * sentence punctuation.
   */
  private static final Pattern BARE_URL =
      Pattern.compile("(?<![(<\\]])(https?://[^\\s<>()\\[\\]]*[^\\s<>()\\[\\].,;:!?'\"])");

  /** How multiple selected tags are combined (AB). */
  private enum TagMatch { ANY, ALL }

  private final transient PublicationsRepository repo = PublicationsProvider.repository();

  private final Grid<Issue> grid = new Grid<>(Issue.class, false);
  private final FilterBar filterBar = new FilterBar();
  private final TextField searchField;
  private final MultiSelectComboBox<Tag> tagFilter;
  private final ComboBox<TagMatch> tagMatch;
  private final MultiSelectComboBox<EditorialState> statusFilter;
  private final Div detail = new Div();

  /** The part currently being dragged in the detail parts grid (F5 reorder). */
  private transient Part draggedPart;

  {
    searchField = filterBar.addText(tr("themen.filter.search", "Search"),
        tr("themen.filter.search.ph", "Title contains…"));
    tagFilter = filterBar.addMultiSelect(tr("themen.filter.tags", "Tags"),
        allTags(), tr("themen.filter.tags.ph", "Any tag"));
    tagFilter.setItemLabelGenerator(Tag::name);
    tagMatch = filterBar.addSingleSelect(tr("themen.filter.tagmatch", "Tag match"),
        TagMatch.values(), tr("themen.filter.tagmatch.ph", "Any (OR)"));
    tagMatch.setItemLabelGenerator(m -> m == TagMatch.ALL
        ? tr("themen.filter.tagmatch.all", "All (AND)")
        : tr("themen.filter.tagmatch.any", "Any (OR)"));
    tagMatch.setValue(TagMatch.ANY);
    statusFilter = filterBar.addMultiSelect(tr("themen.filter.status", "Status"),
        List.of(EditorialState.values()), tr("themen.filter.status.ph", "Any status"));
    statusFilter.setItemLabelGenerator(EditorialState::name);
  }

  public TopicsView() {
    VerticalLayout root = getContent();
    root.setSizeFull();
    root.getStyle().set("gap", "var(--lumo-space-m)");

    root.add(new PageHeader(
        tr("themen.heading", "Topic workspace"),
        tr("themen.subtitle", "Issues, their parts and tags — the entry into the backlog.")));

    // All filters are local to this view (no global/session-scoped filter).
    searchField.addValueChangeListener(e -> refreshMaster());
    tagFilter.addValueChangeListener(e -> refreshMaster());
    tagMatch.addValueChangeListener(e -> refreshMaster());
    statusFilter.addValueChangeListener(e -> refreshMaster());
    filterBar.onClear(this::refreshMaster);

    grid.addColumn(Issue::title).setHeader(tr("themen.col.title", "Title"))
        .setFlexGrow(1).setResizable(true);
    grid.addColumn(i -> i.parts().size()).setHeader(tr("themen.col.parts", "Parts"))
        .setWidth("6em").setFlexGrow(0).setResizable(true);
    grid.addComponentColumn(this::statusSummary).setHeader(tr("themen.col.status", "Status"))
        .setWidth("14em").setFlexGrow(0).setResizable(true);
    grid.setSizeFull();
    grid.asSingleSelect().addValueChangeListener(e -> showDetail(e.getValue()));

    // New-topic action lives directly above the list it creates into (P), not in
    // the far-right page-header actions.
    Button newTopic = new Button(tr("themen.new", "New topic"), VaadinIcon.PLUS.create(),
        e -> openCreateIssueDialog());
    newTopic.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    HorizontalLayout masterToolbar = new HorizontalLayout(newTopic);
    masterToolbar.setWidthFull();
    masterToolbar.setPadding(false);
    masterToolbar.getStyle().set("padding", "var(--lumo-space-s) 0");

    VerticalLayout master = new VerticalLayout(masterToolbar, filterBar, grid);
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
    boolean requireAllTags = tagMatch.getValue() == TagMatch.ALL;
    Set<EditorialState> states = statusFilter.getValue();
    List<Issue> issues = repo.issues().stream()
        .filter(i -> needle.isEmpty() || i.title().toLowerCase().contains(needle))
        // Tags combined with AND (all) or OR (any), per the tag-match selector (AB).
        .filter(i -> PublicationUi.matchesTags(i.tags(), wanted, requireAllTags))
        // Multi-select editorial-state filter, OR-combined: an issue matches if any
        // of its parts is in any of the selected states (AB).
        .filter(i -> states.isEmpty()
            || i.parts().stream().anyMatch(p -> states.contains(p.editorialState())))
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
    title.getStyle().set("margin", "0");
    Button edit = new Button(tr("themen.edit", "Edit"), e ->
        new TopicEditDialog(issue, null, repo, () -> {
          showDetail(issue);
          refreshMaster();
        }).open());
    edit.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
    HorizontalLayout titleRow = new HorizontalLayout(title, edit);
    titleRow.setAlignItems(FlexComponent.Alignment.CENTER);
    detail.add(titleRow);
    if (issue.origin() != null) {
      Span origin = new Span(tr("themen.detail.origin", "Origin: {0}", issue.origin()));
      origin.getStyle().set("color", "var(--lumo-secondary-text-color)");
      origin.getStyle().set("font-size", "var(--lumo-font-size-s)");
      detail.add(origin, new Div());
    }

    // Original body (e.g. the imported ClickUp task text), when present. Rendered
    // as markdown so links become clickable (N); bare URLs are auto-linked.
    if (issue.description() != null && !issue.description().isBlank()) {
      Span originalLabel = new Span(tr("themen.detail.original", "Original text"));
      originalLabel.getStyle().set("font-weight", "600");
      Markdown rendered = new Markdown(linkify(issue.description()));
      Div body = new Div(rendered);
      body.getStyle().set("background", "var(--lumo-contrast-5pct)");
      body.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
      body.getStyle().set("padding", "0 var(--lumo-space-s)");
      body.getStyle().set("max-height", "240px");
      body.getStyle().set("overflow-y", "auto");
      body.getStyle().set("font-size", "var(--lumo-font-size-s)");
      detail.add(originalLabel, body);
    }

    Span tagsLabel = new Span(tr("themen.tags.label", "Tags"));
    tagsLabel.getStyle().set("font-weight", "600");
    tagsLabel.getStyle().set("display", "block");
    tagsLabel.getStyle().set("color", "var(--lumo-secondary-text-color)");
    tagsLabel.getStyle().set("font-size", "var(--lumo-font-size-s)");

    FlexLayout tags = new FlexLayout();
    tags.setFlexWrap(FlexLayout.FlexWrap.WRAP);
    tags.setAlignItems(FlexComponent.Alignment.CENTER);
    tags.getStyle().set("gap", "var(--lumo-space-s)");
    tags.getStyle().set("margin-top", "var(--lumo-space-xs)");
    issue.tags().forEach(t -> tags.add(removableTag(issue, t)));
    Button addTag = new Button(tr("themen.tag.add", "+ Tag"), e -> openAddTagDialog(issue));
    addTag.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
    tags.add(addTag);

    Div tagsSection = new Div(tagsLabel, tags);
    tagsSection.getStyle().set("margin", "var(--lumo-space-m) 0");
    detail.add(tagsSection);

    // Parts section (O) — a labelled, explained block so the "add part" flow is
    // intuitive: what a part is, an empty state when there are none, and a clear
    // primary action that gives feedback and jumps into the new part's editor.
    H4 partsHeading = new H4(tr("themen.parts.heading", "Parts"));
    partsHeading.getStyle().set("margin", "var(--lumo-space-m) 0 0");
    Span partsHint = new Span(tr("themen.parts.hint",
        "A part is a language-neutral unit of this topic. Add a part, then open it to "
            + "write its language versions and plan publications."));
    partsHint.getStyle().set("color", "var(--lumo-secondary-text-color)");
    partsHint.getStyle().set("font-size", "var(--lumo-font-size-s)");
    partsHint.getStyle().set("display", "block");
    detail.add(partsHeading, partsHint);

    if (issue.parts().isEmpty()) {
      detail.add(new EmptyState(VaadinIcon.LIST,
          tr("themen.parts.empty.title", "No parts yet"),
          tr("themen.parts.empty.body",
              "Split this topic into one or more parts to plan and write it.")));
    } else {
      detail.add(partsGrid(issue));
    }

    Button addPart = new Button(tr("themen.part.add", "+ Add part"),
        VaadinIcon.PLUS.create(), e -> addPart(issue));
    addPart.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    addPart.getElement().setAttribute("title",
        tr("themen.part.add.tooltip", "Create a new part and open its editor"));
    detail.add(addPart);
  }

  private Grid<Part> partsGrid(Issue issue) {
    Grid<Part> parts = new Grid<>(Part.class, false);
    parts.addColumn(p -> "#" + p.position()).setHeader("#").setAutoWidth(true);
    parts.addColumn(p -> tr("themen.part", "Part {0}", p.position())).setHeader(tr("themen.col.part", "Part")).setFlexGrow(1);
    parts.addColumn(p -> p.languageVersions().size()).setHeader(tr("themen.col.versions", "Versions")).setAutoWidth(true);
    parts.addComponentColumn(p -> PublicationUi.editorialState(p.editorialState()))
        .setHeader(tr("themen.col.state", "State")).setAutoWidth(true);
    parts.addComponentColumn(this::partActions).setAutoWidth(true);
    parts.setItems(issue.partsInOrder());
    parts.setAllRowsVisible(true);
    // Double-click a part row to open its editor (consistent with the board).
    parts.addItemDoubleClickListener(e -> navigate("part/" + e.getItem().id()));
    // F5 — reorder parts by dragging a row onto another (P0003 ordering).
    parts.setId("parts-grid");
    parts.setRowsDraggable(true);
    parts.setDropMode(GridDropMode.BETWEEN);
    parts.addDragStartListener(e ->
        draggedPart = e.getDraggedItems().isEmpty() ? null : e.getDraggedItems().get(0));
    parts.addDragEndListener(e -> draggedPart = null);
    parts.addDropListener(e -> {
      Part target = e.getDropTargetItem().orElse(null);
      if (draggedPart != null && target != null && target != draggedPart) {
        boolean after = e.getDropLocation() == GridDropLocation.BELOW;
        issue.reorderParts(reorderedParts(issue.partsInOrder(), draggedPart, target, after));
        repo.persist();
        draggedPart = null;
        showDetail(issue);
        refreshMaster();
      }
    });
    return parts;
  }

  /** Adds a part, confirms it, and opens its editor — the natural next step (O). */
  private void addPart(Issue issue) {
    Part created = issue.addPart();
    repo.persist();
    refreshMaster();
    Notification.show(tr("themen.part.added",
        "Part {0} added — opening it to add language versions.", created.position()));
    navigate("part/" + created.id());
  }

  private HorizontalLayout partActions(Part part) {
    Button open = new Button(tr("themen.open", "Open"),
        e -> navigate("part/" + part.id()));
    open.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
    Button history = new Button(tr("themen.history", "History"),
        e -> navigate("history/part/" + part.id()));
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

  /**
   * Computes the new part order when {@code dragged} is moved next to
   * {@code target}. {@code after} inserts below the target (drop location
   * {@code BELOW}), otherwise above it. Pure and side-effect free so the drop
   * handler's reordering can be unit-tested without a live browser; the result
   * is handed to {@link Issue#reorderParts(List)}, which renumbers 1..n.
   */
  public static List<Part> reorderedParts(List<Part> order, Part dragged, Part target, boolean after) {
    List<Part> next = new ArrayList<>(order);
    next.remove(dragged);
    int idx = next.indexOf(target);
    if (idx < 0) {
      next.add(dragged);
    } else {
      next.add(after ? idx + 1 : idx, dragged);
    }
    return next;
  }

  /**
   * Wraps bare {@code http(s)} URLs in markdown autolink brackets so the
   * {@link Markdown} renderer turns them into clickable links, leaving URLs that
   * are already part of a markdown link untouched. Pure and public so the
   * linkification is unit-testable.
   */
  public static String linkify(String text) {
    if (text == null) {
      return "";
    }
    return BARE_URL.matcher(text).replaceAll("<$1>");
  }

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
