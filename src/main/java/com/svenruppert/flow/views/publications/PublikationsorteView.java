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
import com.svenruppert.publications.model.Publikationsort;
import com.svenruppert.publications.model.Sprache;
import com.svenruppert.publications.persistence.PublicationsProvider;
import com.svenruppert.publications.persistence.PublicationsRepository;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * V6 — Publikationsorte (Stammdaten). Pflegt Name und unterstützte Sprachen der
 * Orte. Das Entfernen einer Sprache verlangt Umsicht: bestehende
 * {@link com.svenruppert.publications.model.Veroeffentlichung}en in dieser
 * Sprache an diesem Ort dürfen nicht in einen unzulässigen Zustand geraten —
 * die Maske blockt das mit einem Hinweis. Bedienter Prozess: P0014.
 */
@Route(value = PublikationsorteView.NAV, layout = MainLayout.class)
@RequiresPermission("masterdata:edit")
public class PublikationsorteView extends Composite<VerticalLayout> implements I18nSupport {

  public static final String NAV = "orte";

  private final transient PublicationsRepository repo = PublicationsProvider.repository();
  private final Grid<Publikationsort> grid = new Grid<>(Publikationsort.class, false);

  public PublikationsorteView() {
    VerticalLayout root = getContent();
    root.setSizeFull();
    root.getStyle().set("gap", "var(--lumo-space-m)");

    root.add(new PageHeader(
        tr("orte.heading", "Publication places"),
        tr("orte.subtitle", "Master data — name and supported languages."))
        .withActions(primary(tr("orte.new", "+ Place"), () -> openEditor(null))));

    grid.addColumn(Publikationsort::name).setHeader(tr("orte.col.name", "Place")).setFlexGrow(1);
    grid.addColumn(o -> o.unterstuetzteSprachen().stream().map(Enum::name)
        .sorted().reduce((a, b) -> a + ", " + b).orElse("—"))
        .setHeader(tr("orte.col.langs", "Supported languages")).setAutoWidth(true);
    grid.addColumn(this::inUse).setHeader(tr("orte.col.inuse", "In use")).setAutoWidth(true);
    grid.addComponentColumn(o -> {
      Button edit = new Button(tr("orte.edit", "Edit"), e -> openEditor(o));
      edit.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
      return edit;
    }).setAutoWidth(true);
    grid.setSizeFull();
    root.add(grid);
    root.setFlexGrow(1, grid);

    refresh();
  }

  private long inUse(Publikationsort ort) {
    return repo.alleVeroeffentlichungen().stream()
        .filter(v -> v.ort().id().equals(ort.id()))
        .count();
  }

  private void refresh() {
    grid.setItems(repo.publikationsorte());
  }

  private void openEditor(Publikationsort existing) {
    Dialog dialog = new Dialog();
    dialog.setHeaderTitle(existing == null
        ? tr("orte.new.title", "New place")
        : tr("orte.edit.title", "Edit place"));

    TextField name = new TextField(tr("orte.field.name", "Name"));
    name.setWidthFull();
    MultiSelectComboBox<Sprache> sprachen =
        new MultiSelectComboBox<>(tr("orte.field.langs", "Supported languages"));
    sprachen.setItems(Arrays.asList(Sprache.values()));
    sprachen.setItemLabelGenerator(Enum::name);
    sprachen.setWidthFull();

    if (existing != null) {
      name.setValue(existing.name());
      sprachen.setValue(existing.unterstuetzteSprachen());
    }

    Button save = primary(tr("common.save", "Save"), () -> {
      String value = name.getValue() == null ? "" : name.getValue().strip();
      Set<Sprache> selected = sprachen.getValue();
      if (value.isEmpty() || selected.isEmpty()) {
        name.setInvalid(value.isEmpty());
        Notification.show(tr("orte.required", "Name and at least one language are required."));
        return;
      }
      if (existing == null) {
        repo.neuerPublikationsort(value, new LinkedHashSet<>(selected));
      } else if (!apply(existing, value, selected)) {
        return; // blocked by the language-in-use guard
      }
      dialog.close();
      refresh();
    });
    dialog.add(name, sprachen);
    dialog.getFooter().add(new Button(tr("common.cancel", "Cancel"), e -> dialog.close()), save);
    dialog.open();
    name.focus();
  }

  /** @return false when a removal was blocked because the language is in use. */
  private boolean apply(Publikationsort ort, String name, Set<Sprache> desired) {
    List<Sprache> removed = ort.unterstuetzteSprachen().stream()
        .filter(s -> !desired.contains(s))
        .toList();
    for (Sprache s : removed) {
      boolean used = repo.alleVeroeffentlichungen().stream()
          .anyMatch(v -> v.ort().id().equals(ort.id()) && v.sprache() == s);
      if (used) {
        Notification.show(tr("orte.inuse.block",
            "Cannot remove {0}: publications in that language exist at this place.", s.name()));
        return false;
      }
    }
    ort.setName(name);
    removed.forEach(ort::entferneSprache);
    desired.forEach(ort::ergaenzeSprache);
    repo.persist();
    return true;
  }

  private Button primary(String label, Runnable action) {
    Button b = new Button(label, e -> action.run());
    b.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    return b;
  }
}
