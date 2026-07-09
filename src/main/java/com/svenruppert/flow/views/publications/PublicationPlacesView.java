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
import com.svenruppert.publications.model.Language;
import com.svenruppert.publications.model.PublicationPlace;
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
 * V6 — Publication places (master data). Maintains name and supported languages
 * of the places. Removing a language demands care: existing
 * {@link com.svenruppert.publications.model.Publication}s in that language at that
 * place must not end up in an inadmissible state — the view blocks that with a
 * notification. Served process: P0014.
 */
@Route(value = PublicationPlacesView.NAV, layout = MainLayout.class)
@RequiresPermission("masterdata:edit")
public class PublicationPlacesView extends Composite<VerticalLayout> implements I18nSupport {

  public static final String NAV = "orte";

  private static final long serialVersionUID = 1L;

  private final transient PublicationsRepository repo = PublicationsProvider.repository();
  private final Grid<PublicationPlace> grid = new Grid<>(PublicationPlace.class, false);

  public PublicationPlacesView() {
    VerticalLayout root = getContent();
    root.setSizeFull();
    root.getStyle().set("gap", "var(--lumo-space-m)");

    root.add(new PageHeader(
        tr("orte.heading", "Publication places"),
        tr("orte.subtitle", "Master data — name and supported languages."))
        .withActions(primary(tr("orte.new", "+ Place"), () -> openEditor(null))));

    grid.addColumn(PublicationPlace::name).setHeader(tr("orte.col.name", "Place")).setFlexGrow(1);
    grid.addColumn(o -> o.supportedLanguages().stream().map(Enum::name)
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

  private long inUse(PublicationPlace place) {
    return repo.allPublications().stream()
        .filter(v -> v.place().id().equals(place.id()))
        .count();
  }

  private void refresh() {
    grid.setItems(repo.publicationPlaces());
  }

  private void openEditor(PublicationPlace existing) {
    Dialog dialog = new Dialog();
    dialog.setHeaderTitle(existing == null
        ? tr("orte.new.title", "New place")
        : tr("orte.edit.title", "Edit place"));

    TextField name = new TextField(tr("orte.field.name", "Name"));
    name.setWidthFull();
    MultiSelectComboBox<Language> languages =
        new MultiSelectComboBox<>(tr("orte.field.langs", "Supported languages"));
    languages.setItems(Arrays.asList(Language.values()));
    languages.setItemLabelGenerator(Enum::name);
    languages.setWidthFull();

    if (existing != null) {
      name.setValue(existing.name());
      languages.setValue(existing.supportedLanguages());
    }

    Button save = primary(tr("common.save", "Save"), () -> {
      String value = name.getValue() == null ? "" : name.getValue().strip();
      Set<Language> selected = languages.getValue();
      if (value.isEmpty() || selected.isEmpty()) {
        name.setInvalid(value.isEmpty());
        Notification.show(tr("orte.required", "Name and at least one language are required."));
        return;
      }
      if (existing == null) {
        repo.createPublicationPlace(value, new LinkedHashSet<>(selected));
      } else if (!apply(existing, value, selected)) {
        return; // blocked by the language-in-use guard
      }
      dialog.close();
      refresh();
    });
    dialog.add(name, languages);
    dialog.getFooter().add(new Button(tr("common.cancel", "Cancel"), e -> dialog.close()), save);
    dialog.open();
    name.focus();
  }

  /** @return false when a removal was blocked because the language is in use. */
  private boolean apply(PublicationPlace place, String name, Set<Language> desired) {
    List<Language> removed = place.supportedLanguages().stream()
        .filter(l -> !desired.contains(l))
        .toList();
    for (Language l : removed) {
      boolean used = repo.allPublications().stream()
          .anyMatch(v -> v.place().id().equals(place.id()) && v.language() == l);
      if (used) {
        Notification.show(tr("orte.inuse.block",
            "Cannot remove {0}: publications in that language exist at this place.", l.name()));
        return false;
      }
    }
    place.setName(name);
    removed.forEach(place::removeLanguage);
    desired.forEach(place::addLanguage);
    repo.persist();
    return true;
  }

  private Button primary(String label, Runnable action) {
    Button b = new Button(label, e -> action.run());
    b.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    return b;
  }
}
