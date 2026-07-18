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
import com.svenruppert.flow.views.ui.FilterBar;
import com.svenruppert.flow.views.ui.PageHeader;
import com.svenruppert.flow.views.ui.TemplateBrand;
import com.svenruppert.jsentinel.authorization.api.SubjectStores;
import com.svenruppert.publications.model.EditorialState;
import com.svenruppert.publications.model.Part;
import com.svenruppert.publications.model.Tag;
import com.svenruppert.publications.persistence.PublicationsProvider;
import com.svenruppert.publications.persistence.PublicationsRepository;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.dnd.DragSource;
import com.vaadin.flow.component.dnd.DropTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.router.Route;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * V3 — Editorial board. Shows the editorial progress of all {@link Part}s in two
 * switchable views:
 * <ul>
 *   <li><b>Board</b> — one Kanban column per {@link EditorialState}; a card is
 *       dragged onto another column to advance its state (the column already
 *       denotes the state, so the card carries no redundant selector).</li>
 *   <li><b>Table</b> — a tree grouped by {@link EditorialState}, within each group
 *       sorted by topic name; a read overview complementing the board.</li>
 * </ul>
 * Served processes: P0007, P0020 (editorial dimension).
 */
@Route(value = EditorialBoardView.NAV, layout = MainLayout.class)
@VisibleFor(AuthorizationRole.USER)
public class EditorialBoardView extends Composite<VerticalLayout> implements I18nSupport {

  public static final String NAV = "redaktion";

  private static final long serialVersionUID = 1L;

  private final transient PublicationsRepository repo = PublicationsProvider.repository();

  private final FlexLayout board = new FlexLayout();
  private final TreeGrid<BoardRow> table = new TreeGrid<>();
  private final Tab boardTab = new Tab(tr("tafel.view.board", "Board"));
  private final Tab tableTab = new Tab(tr("tafel.view.table", "Table"));
  private final Tabs viewTabs = new Tabs(boardTab, tableTab);

  /** How multiple selected tags are combined (X). */
  private enum TagMatch { ANY, ALL }

  // Complex search filters for the board (W).
  private final FilterBar filterBar = new FilterBar();
  private final TextField search;
  private final MultiSelectComboBox<Tag> tagFilter;
  private final ComboBox<TagMatch> tagMatch;
  private final MultiSelectComboBox<EditorialState> stateFilter;

  /** The part currently being dragged; set on drag start, cleared on drag end/drop. */
  private transient Part draggedPart;

  {
    search = filterBar.addText(tr("tafel.filter.search", "Search"),
        tr("tafel.filter.search.ph", "Topic title…"));
    tagFilter = filterBar.addMultiSelect(tr("tafel.filter.tags", "Tags"),
        allTags(), tr("tafel.filter.tags.ph", "Any tag"));
    tagFilter.setItemLabelGenerator(Tag::name);
    tagMatch = filterBar.addSingleSelect(tr("tafel.filter.tagmatch", "Tag match"),
        TagMatch.values(), tr("tafel.filter.tagmatch.ph", "Any (OR)"));
    tagMatch.setItemLabelGenerator(m -> m == TagMatch.ALL
        ? tr("tafel.filter.tagmatch.all", "All (AND)")
        : tr("tafel.filter.tagmatch.any", "Any (OR)"));
    tagMatch.setValue(TagMatch.ANY);
    stateFilter = filterBar.addMultiSelect(tr("tafel.filter.state", "State"),
        List.of(EditorialState.values()), tr("tafel.filter.state.ph", "Any state"));
    stateFilter.setItemLabelGenerator(EditorialState::name);
  }

  public EditorialBoardView() {
    VerticalLayout root = getContent();
    root.setSizeFull();
    root.getStyle().set("gap", "var(--lumo-space-m)");

    root.add(new PageHeader(
        tr("tafel.heading", "Editorial board"),
        tr("tafel.subtitle",
            "Editorial state per part — drag a card to another column, double-click to edit, or switch to the table view.")));

    search.addValueChangeListener(e -> refresh());
    tagFilter.addValueChangeListener(e -> refresh());
    tagMatch.addValueChangeListener(e -> refresh());
    stateFilter.addValueChangeListener(e -> refresh());
    filterBar.onClear(this::refresh);
    root.add(filterBar);

    viewTabs.addSelectedChangeListener(e -> updateVisibleView());
    root.add(viewTabs);

    board.setWidthFull();
    board.getStyle().set("gap", "var(--lumo-space-m)");
    board.getStyle().set("overflow-x", "auto");
    board.setFlexWrap(FlexLayout.FlexWrap.NOWRAP);
    root.add(board);
    root.setFlexGrow(1, board);

    table.addHierarchyColumn(this::rowLabel)
        .setHeader(tr("tafel.col.name", "Topic / status")).setFlexGrow(1);
    table.addColumn(this::rowVersions)
        .setHeader(tr("tafel.col.versions", "Versions")).setAutoWidth(true);
    // Double-click a table row to see/edit its topic and status (K).
    table.addItemDoubleClickListener(e -> {
      if (e.getItem() instanceof PartRow partRow) {
        openEditor(partRow.part());
      }
    });
    table.setSizeFull();
    table.setVisible(false);
    root.add(table);
    root.setFlexGrow(1, table);

    refresh();
  }

  private void updateVisibleView() {
    boolean showBoard = viewTabs.getSelectedTab() == boardTab;
    board.setVisible(showBoard);
    table.setVisible(!showBoard);
  }

  private void refresh() {
    List<Part> all = filteredParts();

    // Only the selected states get a column/group; empty selection shows all.
    Set<EditorialState> wantedStates = stateFilter.getValue();
    List<EditorialState> visibleStates = wantedStates.isEmpty()
        ? List.of(EditorialState.values())
        : Arrays.stream(EditorialState.values()).filter(wantedStates::contains).toList();

    // ── Kanban board ──
    board.removeAll();
    for (EditorialState state : visibleStates) {
      List<Part> inColumn = all.stream()
          .filter(p -> p.editorialState() == state)
          .toList();
      board.add(column(state, inColumn));
    }

    // ── Table (grouped by status, sorted by name) ──
    List<BoardRow> groups = new ArrayList<>();
    for (EditorialState state : visibleStates) {
      List<Part> inState = partsInStateSortedByName(all, state);
      if (!inState.isEmpty()) {
        groups.add(new GroupRow(state, inState.size()));
      }
    }
    table.setItems(groups, this::childrenOf);
    table.expand(groups);
  }

  /**
   * Parts matching the board's local filters (W): the free-text topic search and
   * the tag filter (AND/OR). The state filter is applied per column/group in
   * {@link #refresh()}. Recomputed on demand.
   */
  private List<Part> filteredParts() {
    String needle = search.getValue() == null ? "" : search.getValue().strip().toLowerCase();
    Set<Tag> wantedTags = tagFilter.getValue();
    boolean requireAll = tagMatch.getValue() == TagMatch.ALL;
    return repo.issues().stream()
        .filter(i -> needle.isEmpty() || i.title().toLowerCase().contains(needle))
        .filter(i -> matchesTags(i.tags(), wantedTags, requireAll))
        .flatMap(i -> i.parts().stream())
        .toList();
  }

  /**
   * Whether {@code issueTags} satisfies the {@code wanted} tag selection: with
   * {@code requireAll} the issue must carry <em>all</em> selected tags (AND),
   * otherwise <em>any</em> of them (OR). An empty selection always matches. Pure
   * and public so the AND/OR semantics are unit-testable.
   */
  public static boolean matchesTags(Set<Tag> issueTags, Set<Tag> wanted, boolean requireAll) {
    if (wanted.isEmpty()) {
      return true;
    }
    return requireAll ? issueTags.containsAll(wanted) : issueTags.stream().anyMatch(wanted::contains);
  }

  /** All tags across the issues, ordered case-insensitively (for the tag filter). */
  private List<Tag> allTags() {
    return repo.issues().stream()
        .flatMap(i -> i.tags().stream())
        .collect(java.util.stream.Collectors.toCollection(
            () -> new TreeSet<>((a, b) -> a.name().compareToIgnoreCase(b.name()))))
        .stream().toList();
  }

  // ── Kanban board ─────────────────────────────────────────────────────────

  private Div column(EditorialState state, List<Part> parts) {
    Div col = new Div();
    col.setId("col-" + state.name());
    // Each column is a drop target: dropping a dragged card here advances the
    // part's editorial state to this column's state.
    DropTarget<Div> dropTarget = DropTarget.create(col);
    dropTarget.addDropListener(e -> onDrop(state));
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
    card.setId("card-" + part.id());
    card.addClassName(TemplateBrand.CSS_CARD);
    card.getStyle().set("padding", "var(--lumo-space-s)");
    card.getStyle().set("cursor", "grab");
    // Each card is a drag source; the dragged part is tracked for the drop. The
    // column denotes the state, so the card needs no state selector.
    DragSource<Div> dragSource = DragSource.create(card);
    dragSource.setDragData(part);
    dragSource.addDragStartListener(e -> draggedPart = part);
    dragSource.addDragEndListener(e -> draggedPart = null);
    // Double-click a card to see/edit its topic and status (J).
    card.addDoubleClickListener(e -> openEditor(part));

    String prefix = part.issue() != null ? part.issue().title() : "";
    Span title = new Span(prefix);
    title.getStyle().set("font-weight", "600");
    title.getStyle().set("display", "block");
    Span info = new Span(tr("tafel.part", "Part {0}", part.position())
        + " · " + part.languageVersions().size() + " " + tr("tafel.versions", "versions"));
    info.getStyle().set("color", "var(--lumo-secondary-text-color)");
    info.getStyle().set("font-size", "var(--lumo-font-size-s)");
    info.getStyle().set("display", "block");

    card.add(title, info);
    return card;
  }

  /**
   * Handles a card dropped onto the column for {@code target}: advances the
   * dragged part's editorial state (unless it is already there), persists and
   * re-renders both views.
   */
  private void onDrop(EditorialState target) {
    if (draggedPart != null && draggedPart.editorialState() != target) {
      draggedPart.changeState(target, actor());
      repo.persist();
      draggedPart = null;
      refresh();
    }
  }

  // ── Table tree ───────────────────────────────────────────────────────────

  private Collection<BoardRow> childrenOf(BoardRow row) {
    if (row instanceof GroupRow group) {
      return partsInStateSortedByName(filteredParts(), group.state()).stream()
          .map(part -> (BoardRow) new PartRow(part))
          .toList();
    }
    return List.of();
  }

  private String rowLabel(BoardRow row) {
    if (row instanceof GroupRow group) {
      return group.state().name() + " (" + group.count() + ")";
    }
    Part part = ((PartRow) row).part();
    return partName(part) + " · " + tr("tafel.part", "Part {0}", part.position());
  }

  private String rowVersions(BoardRow row) {
    if (row instanceof PartRow partRow) {
      return String.valueOf(partRow.part().languageVersions().size());
    }
    return "";
  }

  /**
   * The parts in {@code state}, sorted by topic name (case-insensitive), ties
   * broken by position. Pure and public so the grouping/sorting can be unit-tested.
   */
  public static List<Part> partsInStateSortedByName(List<Part> all, EditorialState state) {
    return all.stream()
        .filter(p -> p.editorialState() == state)
        .sorted(Comparator.comparing(EditorialBoardView::partName, String.CASE_INSENSITIVE_ORDER)
            .thenComparingInt(Part::position))
        .toList();
  }

  private static String partName(Part part) {
    return part.issue() != null ? part.issue().title() : "";
  }

  /** Opens the shared topic/part editor for {@code part} (double-click, J/K). */
  private void openEditor(Part part) {
    if (part != null && part.issue() != null) {
      new TopicEditDialog(part.issue(), part, repo, this::refresh).open();
    }
  }

  private static String actor() {
    return SubjectStores.subjectStore().currentSubject(AppUser.class)
        .map(AppUser::name).orElse("system");
  }

  // ── table row model ────────────────────────────────────────────────────────

  private sealed interface BoardRow permits GroupRow, PartRow {
  }

  private record GroupRow(EditorialState state, int count) implements BoardRow {
  }

  private record PartRow(Part part) implements BoardRow {
  }
}
