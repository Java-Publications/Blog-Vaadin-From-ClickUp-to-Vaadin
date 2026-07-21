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
import com.svenruppert.publications.model.Client;
import com.svenruppert.publications.persistence.PublicationsProvider;
import com.svenruppert.publications.persistence.PublicationsRepository;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

/**
 * Clients (Auftraggeber) master data — a fixed, maintainable list. A
 * {@link com.svenruppert.publications.model.Publication} references a client by
 * name (picked from this list), so the set of clients is curated here rather than
 * typed freely on each publication. Renaming a client propagates to the
 * publications that use the old name.
 */
@Route(value = ClientsView.NAV, layout = MainLayout.class)
@RequiresPermission("masterdata:edit")
public class ClientsView extends Composite<VerticalLayout> implements I18nSupport {

  public static final String NAV = "clients";

  private static final long serialVersionUID = 1L;

  private final transient PublicationsRepository repo = PublicationsProvider.repository();
  private final Grid<Client> grid = new Grid<>(Client.class, false);

  public ClientsView() {
    VerticalLayout root = getContent();
    root.setSizeFull();
    root.getStyle().set("gap", "var(--lumo-space-m)");

    root.add(new PageHeader(
        tr("auftraggeber.heading", "Clients"),
        tr("auftraggeber.subtitle", "Master data — the fixed list of clients to pick from."))
        .withActions(primary(tr("auftraggeber.new", "+ Client"), () -> openEditor(null))));

    grid.addColumn(Client::name).setHeader(tr("auftraggeber.col.name", "Client"))
        .setFlexGrow(1).setResizable(true);
    grid.addColumn(this::inUse).setHeader(tr("auftraggeber.col.inuse", "In use"))
        .setWidth("8em").setFlexGrow(0).setResizable(true);
    grid.addComponentColumn(c -> {
      Button edit = new Button(tr("auftraggeber.edit", "Edit"), e -> openEditor(c));
      edit.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
      return edit;
    }).setWidth("7em").setFlexGrow(0);
    grid.setSizeFull();
    root.add(grid);
    root.setFlexGrow(1, grid);

    refresh();
  }

  private long inUse(Client client) {
    return repo.allPublications().stream()
        .filter(v -> client.name().equals(v.client()))
        .count();
  }

  private void refresh() {
    grid.setItems(repo.clients());
  }

  private void openEditor(Client existing) {
    Dialog dialog = new Dialog();
    dialog.setHeaderTitle(existing == null
        ? tr("auftraggeber.new.title", "New client")
        : tr("auftraggeber.edit.title", "Edit client"));

    TextField name = new TextField(tr("auftraggeber.field.name", "Name"));
    name.setWidthFull();
    if (existing != null) {
      name.setValue(existing.name());
    }

    Button save = primary(tr("common.save", "Save"), () -> {
      String value = name.getValue() == null ? "" : name.getValue().strip();
      if (value.isEmpty()) {
        name.setInvalid(true);
        name.setErrorMessage(tr("auftraggeber.required", "Name required"));
        return;
      }
      if (existing == null) {
        repo.createClient(value);
      } else {
        rename(existing, value);
      }
      dialog.close();
      refresh();
    });
    dialog.add(name);
    dialog.getFooter().add(new Button(tr("common.cancel", "Cancel"), e -> dialog.close()), save);
    dialog.open();
    name.focus();
  }

  /** Renames a client and propagates the new name to publications using the old one. */
  private void rename(Client client, String newName) {
    String old = client.name();
    if (old.equals(newName)) {
      return;
    }
    client.setName(newName);
    repo.allPublications().stream()
        .filter(v -> old.equals(v.client()))
        .forEach(v -> v.setClient(newName));
    repo.persist();
    Notification.show(tr("auftraggeber.renamed", "Renamed to {0}.", newName));
  }

  private Button primary(String label, Runnable action) {
    Button b = new Button(label, e -> action.run());
    b.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    return b;
  }
}
