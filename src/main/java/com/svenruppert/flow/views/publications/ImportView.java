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
import com.svenruppert.flow.views.MainLayout;
import com.svenruppert.flow.views.ui.PageHeader;
import com.svenruppert.jsentinel.authorization.annotations.RequiresPermission;
import com.svenruppert.publications.importetl.ClickUpImportService;
import com.svenruppert.publications.importetl.ImportReport;
import com.svenruppert.publications.persistence.PublicationsProvider;
import com.svenruppert.publications.persistence.PublicationsRepository;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.Composite;
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

import java.util.Map;

/**
 * V7 — Import-Konsole (ETL). Steuert den idempotenten Import aus ClickUp in drei
 * Schritten: Extrahieren (echte API), Transformieren &amp; Laden, Wiederholungslauf
 * (erzeugt keine Dubletten). Zeigt das Protokoll des letzten Laufs und die
 * Verteilung des konflierten Ursprungsstatus auf die entflochtene Dimension.
 * Bediente Prozesse: P0016–P0018. Bewusst schmucklos (Wegwerfmodul-Nähe).
 */
@Route(value = ImportView.NAV, layout = MainLayout.class)
@RequiresPermission("publications:import")
public class ImportView extends Composite<VerticalLayout> implements I18nSupport {

  public static final String NAV = "import";

  private final transient ClickUpImportService service = new ClickUpImportService();
  private final transient PublicationsRepository repo = PublicationsProvider.repository();

  private final PasswordField token = new PasswordField(tr("import.token", "ClickUp API token"));
  private final TextField listId = new TextField(tr("import.list", "List id"));
  private final ProgressBar progress = new ProgressBar();
  private final Div protokoll = new Div();
  private final Grid<Map.Entry<String, Integer>> verteilung = new Grid<>();

  private transient String lastRaw;

  public ImportView() {
    VerticalLayout root = getContent();
    root.setSizeFull();
    root.getStyle().set("gap", "var(--lumo-space-m)");

    root.add(new PageHeader(
        tr("import.heading", "Import console"),
        tr("import.subtitle", "Idempotent ClickUp import in three steps — a throwaway module.")));

    token.setValue(System.getProperty("clickup.token", ""));
    listId.setValue(System.getProperty("clickup.listId", ""));
    token.setWidth("320px");
    HorizontalLayout config = new HorizontalLayout(token, listId);
    root.add(config);

    Button extract = new Button(tr("import.extract", "① Extract"), e -> onExtract());
    extract.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    Button load = new Button(tr("import.load", "② Transform & load"), e -> onLoad());
    Button repeat = new Button(tr("import.repeat", "③ Repeat run"), e -> onLoad());
    root.add(new HorizontalLayout(extract, load, repeat));

    progress.setIndeterminate(true);
    progress.setVisible(false);
    root.add(progress);

    protokoll.getStyle().set("color", "var(--lumo-secondary-text-color)");
    protokoll.setText(tr("import.none", "No run yet. Start with ①."));
    root.add(protokoll);

    verteilung.addColumn(Map.Entry::getKey)
        .setHeader(tr("import.col.mapping", "ClickUp status → dimension")).setFlexGrow(1);
    verteilung.addColumn(Map.Entry::getValue)
        .setHeader(tr("import.col.count", "Count")).setAutoWidth(true);
    verteilung.setAllRowsVisible(true);
    root.add(new Span(tr("import.dist", "Status disentanglement — ClickUp → dimension")));
    root.add(verteilung);

    root.setFlexGrow(1, verteilung);
  }

  private void onExtract() {
    String t = token.getValue() == null ? "" : token.getValue().strip();
    String l = listId.getValue() == null ? "" : listId.getValue().strip();
    if (t.isEmpty() || l.isEmpty()) {
      Notification.show(tr("import.needcfg", "Enter the API token and list id first."));
      return;
    }
    progress.setVisible(true);
    try {
      lastRaw = service.extract(t, l);
      protokoll.setText(tr("import.extracted", "Extracted {0} bytes. Now run ②.", lastRaw.length()));
    } catch (Exception ex) {
      Notification.show(tr("import.extractfail", "Extract failed: {0}", String.valueOf(ex.getMessage())));
    } finally {
      progress.setVisible(false);
    }
  }

  private void onLoad() {
    if (lastRaw == null || lastRaw.isBlank()) {
      Notification.show(tr("import.noraw", "Nothing extracted yet — run ① first."));
      return;
    }
    try {
      ImportReport report = service.transformAndLoad(lastRaw, repo);
      protokoll.setText(tr("import.report",
          "Created {0} · updated {1} · skipped {2}.",
          report.angelegt(), report.aktualisiert(), report.uebersprungen()));
      verteilung.setItems(report.statusVerteilung().entrySet());
    } catch (RuntimeException ex) {
      Notification.show(tr("import.loadfail", "Transform/load failed: {0}",
          String.valueOf(ex.getMessage())));
    }
  }
}
