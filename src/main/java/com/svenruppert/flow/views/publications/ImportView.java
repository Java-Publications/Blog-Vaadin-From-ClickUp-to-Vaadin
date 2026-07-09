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
import com.svenruppert.flow.security.storage.AppStoragePaths;
import com.svenruppert.flow.views.MainLayout;
import com.svenruppert.flow.views.ui.PageHeader;
import com.svenruppert.jsentinel.authorization.annotations.RequiresPermission;
import com.svenruppert.publications.importetl.ClickUpImportService;
import com.svenruppert.publications.importetl.ImportReport;
import com.svenruppert.publications.importetl.RawImportStore;
import com.svenruppert.publications.persistence.PublicationsProvider;
import com.svenruppert.publications.persistence.PublicationsRepository;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.UIDetachedException;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.Command;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Map;

/**
 * V7 — Import console (ETL). Controls the idempotent ClickUp import in three
 * steps: extract (real API, all pages), transform &amp; load, repeat run (creates
 * no duplicates). The two long-running steps run off the UI thread and report
 * live progress through a per-action progress bar and a scrolling log, so the
 * user always sees what is happening; a result panel summarises the run and links
 * into the topic workspace where the imported topics land. Served processes:
 * P0016–P0018.
 */
@Route(value = ImportView.NAV, layout = MainLayout.class)
@RequiresPermission("publications:import")
public class ImportView extends Composite<VerticalLayout> implements I18nSupport {

  public static final String NAV = "import";

  private static final long serialVersionUID = 1L;

  private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss");

  private final transient ClickUpImportService service = new ClickUpImportService();
  private final transient PublicationsRepository repo = PublicationsProvider.repository();
  private final transient RawImportStore rawStore = new RawImportStore(AppStoragePaths.importDir());

  private final PasswordField token = new PasswordField(tr("import.token", "ClickUp API token"));
  private final TextField listId = new TextField(tr("import.list", "List id"));
  private final Button extractBtn = new Button(tr("import.extract", "① Extract"), e -> onExtract());
  private final Button loadBtn = new Button(tr("import.load", "② Transform & load"), e -> onLoad());
  private final Button repeatBtn = new Button(tr("import.repeat", "③ Repeat run"), e -> onLoad());
  private final ProgressBar progress = new ProgressBar();
  private final Span lastExtraction = new Span();
  private final Div resultPanel = new Div();
  private final Div console = new Div();
  private final Grid<Map.Entry<String, Integer>> distribution = new Grid<>();

  private transient String lastRaw;

  public ImportView() {
    VerticalLayout root = getContent();
    root.setSizeFull();
    root.getStyle().set("gap", "var(--lumo-space-m)");

    root.add(new PageHeader(
        tr("import.heading", "Import console"),
        tr("import.subtitle", "Idempotent ClickUp import in three steps — extract, transform & load, repeat.")));

    token.setValue(System.getProperty("clickup.token", ""));
    listId.setValue(System.getProperty("clickup.listId", ""));
    token.setWidth("320px");
    root.add(new HorizontalLayout(token, listId));

    extractBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    root.add(new HorizontalLayout(extractBtn, loadBtn, repeatBtn));

    progress.setVisible(false);
    root.add(progress);

    lastExtraction.getStyle().set("color", "var(--lumo-secondary-text-color)");
    lastExtraction.getStyle().set("font-size", "var(--lumo-font-size-s)");
    root.add(lastExtraction);

    resultPanel.setVisible(false);
    resultPanel.getStyle().set("padding", "var(--lumo-space-s) var(--lumo-space-m)");
    resultPanel.getStyle().set("background", "var(--lumo-primary-color-10pct)");
    resultPanel.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
    root.add(resultPanel);

    // Scrolling progress log — the running narrative of the import.
    console.getStyle().set("font-family",
        "'IBM Plex Mono', ui-monospace, SFMono-Regular, Menlo, Consolas, monospace");
    console.getStyle().set("font-size", "var(--lumo-font-size-xs)");
    console.getStyle().set("background", "var(--lumo-contrast-5pct)");
    console.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
    console.getStyle().set("padding", "var(--lumo-space-s)");
    console.getStyle().set("max-height", "180px");
    console.getStyle().set("overflow-y", "auto");
    console.setWidthFull();
    root.add(new Span(tr("import.console", "Progress log")));
    root.add(console);

    distribution.addColumn(Map.Entry::getKey)
        .setHeader(tr("import.col.mapping", "ClickUp status → dimension")).setFlexGrow(1);
    distribution.addColumn(Map.Entry::getValue)
        .setHeader(tr("import.col.count", "Count")).setAutoWidth(true);
    distribution.setAllRowsVisible(true);
    root.add(new Span(tr("import.dist", "Status disentanglement — ClickUp → dimension")));
    root.add(distribution);

    root.setFlexGrow(1, distribution);

    line(tr("import.none", "No run yet. Start with ①."));
    loadLastRaw();
  }

  /**
   * On open, adopts the last raw extract cached on disk (if any) so a
   * transform/load can run against it without re-fetching — the import survives
   * a restart — and shows when that extract was taken.
   */
  private void loadLastRaw() {
    rawStore.loadRaw().ifPresent(raw -> {
      lastRaw = raw;
      line(tr("import.loadedraw",
          "Loaded {0} bytes from the last local extract — run ② to load.", raw.length()));
    });
    renderLastExtraction();
  }

  private void renderLastExtraction() {
    lastExtraction.setText(rawStore.lastExtraction()
        .map(when -> tr("import.lastextract", "Last extraction: {0}", formatTimestamp(when)))
        .orElseGet(() -> tr("import.lastextract.never", "No local extract yet.")));
  }

  private static String formatTimestamp(Instant when) {
    return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
        .withZone(ZoneId.systemDefault())
        .format(when);
  }

  // ── ① extract ──────────────────────────────────────────────────────────────

  private void onExtract() {
    String t = token.getValue() == null ? "" : token.getValue().strip();
    String l = listId.getValue() == null ? "" : listId.getValue().strip();
    if (t.isEmpty() || l.isEmpty()) {
      Notification.show(tr("import.needcfg", "Enter the API token and list id first."));
      return;
    }
    UI ui = UI.getCurrent();
    beginAction(true);
    line(tr("import.extracting", "Extracting all pages from list {0} …", l));
    Thread worker = new Thread(() -> {
      try {
        String raw = service.extract(t, l);
        // Persist the raw extract locally so the transform/load survives a restart.
        rawStore.save(raw, Instant.now());
        push(ui, () -> {
          lastRaw = raw;
          renderLastExtraction();
          line(tr("import.extracted", "Extracted {0} bytes; cached locally. Now run ②.", raw.length()));
          endAction();
        });
      } catch (Exception ex) {
        String msg = String.valueOf(ex.getMessage());
        push(ui, () -> {
          line(tr("import.extractfail", "Extract failed: {0}", msg));
          endAction();
        });
      }
    }, "clickup-extract");
    worker.setDaemon(true);
    worker.start();
  }

  // ── ② / ③ transform & load ──────────────────────────────────────────────────

  private void onLoad() {
    if (lastRaw == null || lastRaw.isBlank()) {
      Notification.show(tr("import.noraw", "Nothing extracted yet — run ① first."));
      return;
    }
    UI ui = UI.getCurrent();
    String raw = lastRaw;
    beginAction(false);
    line(tr("import.transforming", "Transforming and saving …"));
    Thread worker = new Thread(() -> {
      try {
        ImportReport report = service.transformAndLoad(raw, repo, (done, total) -> {
          // Throttle to ~50 updates so a large import does not flood the push channel.
          if (done == total || done % Math.max(1, total / 50) == 0) {
            push(ui, () -> progress.setValue(total == 0 ? 1d : (double) done / total));
          }
        });
        push(ui, () -> {
          line(tr("import.report", "Created {0} · updated {1} · skipped {2}.",
              report.created(), report.updated(), report.skipped()));
          distribution.setItems(report.statusDistribution().entrySet());
          showResult(report);
          endAction();
        });
      } catch (RuntimeException ex) {
        String msg = String.valueOf(ex.getMessage());
        push(ui, () -> {
          line(tr("import.loadfail", "Transform/load failed: {0}", msg));
          endAction();
        });
      }
    }, "clickup-load");
    worker.setDaemon(true);
    worker.start();
  }

  private void showResult(ImportReport report) {
    resultPanel.removeAll();
    int processed = report.created() + report.skipped();
    if (processed == 0) {
      resultPanel.add(new Span(tr("import.result.empty", "The extract contained no tasks.")));
    } else {
      Span summary = new Span(tr("import.result.count",
          "{0} imported · {1} already present · {2} topics in the workspace now.",
          report.created(), report.skipped(), repo.issues().size()));
      summary.getStyle().set("font-weight", "600");
      Button toTopics = new Button(tr("import.open.topics", "Open topic workspace"),
          e -> UI.getCurrent().navigate(TopicsView.class));
      toTopics.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
      resultPanel.add(summary, new Div(), toTopics);
    }
    resultPanel.setVisible(true);
  }

  // ── UI helpers ───────────────────────────────────────────────────────────────

  /** Appends a timestamped line to the progress log and scrolls it into view. */
  private void line(String text) {
    Div entry = new Div();
    entry.setText(LocalTime.now().format(CLOCK) + "  " + text);
    console.add(entry);
    console.getElement().executeJs("this.scrollTop = this.scrollHeight;");
  }

  private void beginAction(boolean indeterminate) {
    setButtonsEnabled(false);
    resultPanel.setVisible(false);
    progress.setIndeterminate(indeterminate);
    if (!indeterminate) {
      progress.setValue(0d);
    }
    progress.setVisible(true);
  }

  private void endAction() {
    setButtonsEnabled(true);
    progress.setVisible(false);
  }

  private void setButtonsEnabled(boolean enabled) {
    extractBtn.setEnabled(enabled);
    loadBtn.setEnabled(enabled);
    repeatBtn.setEnabled(enabled);
  }

  /** Runs {@code action} on the UI thread via server push, tolerating a detached UI. */
  private static void push(UI ui, Command action) {
    if (ui == null) {
      return;
    }
    try {
      ui.access(action);
    } catch (UIDetachedException ignored) {
      // the user navigated away mid-import — nothing to update
    }
  }
}
