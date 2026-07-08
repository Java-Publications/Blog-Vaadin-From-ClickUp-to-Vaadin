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
import com.svenruppert.flow.views.ui.PageHeader;
import com.svenruppert.publications.model.Publikationsort;
import com.svenruppert.publications.model.Sprache;
import com.svenruppert.publications.model.Sprachfassung;
import com.svenruppert.publications.model.Teil;
import com.svenruppert.publications.model.Veroeffentlichung;
import com.svenruppert.publications.persistence.PublicationsProvider;
import com.svenruppert.publications.persistence.PublicationsRepository;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * V2 — Sprachfassungs-Editor. Zu einem {@link Teil} wird je {@link Sprache} das
 * Manuskript samt geplanter Zeichenzahl gepflegt; zugleich ist die Maske der
 * Absprung in die Planung einer {@link Veroeffentlichung} (Sprachregel-gefiltert).
 *
 * <p>Erste parametrisierte Route im Projekt: {@code teil/&lt;uuid&gt;}. Bediente
 * Prozesse: P0005, P0006; Absprung zu P0008/P0012.
 */
@Route(value = SprachfassungView.NAV, layout = MainLayout.class)
@VisibleFor(AuthorizationRole.USER)
public class SprachfassungView extends Composite<VerticalLayout>
    implements HasUrlParameter<String>, I18nSupport {

  public static final String NAV = "teil";

  private final transient PublicationsRepository repo = PublicationsProvider.repository();
  private final Div body = new Div();

  private transient Teil teil;

  public SprachfassungView() {
    VerticalLayout root = getContent();
    root.setSizeFull();
    root.getStyle().set("gap", "var(--lumo-space-m)");
    body.setSizeFull();
    root.add(body);
  }

  @Override
  public void setParameter(BeforeEvent event, String teilId) {
    this.teil = parse(teilId).flatMap(repo::findTeil).orElse(null);
    render();
  }

  private static java.util.Optional<UUID> parse(String id) {
    try {
      return java.util.Optional.of(UUID.fromString(id));
    } catch (IllegalArgumentException e) {
      return java.util.Optional.empty();
    }
  }

  private void render() {
    body.removeAll();
    if (teil == null) {
      body.add(new EmptyState(VaadinIcon.FILE_TEXT_O,
          tr("fassung.notfound.title", "Part not found"),
          tr("fassung.notfound.body", "Open a part from the topic workspace.")));
      return;
    }

    String prefix = teil.issue() != null ? teil.issue().titel() : "";
    body.add(new PageHeader(
        tr("fassung.heading", "Language versions"),
        prefix + " · " + tr("fassung.part", "Part {0}", teil.reihenfolge())));

    Tabs tabs = new Tabs();
    Div editor = new Div();
    editor.setWidthFull();

    List<Sprachfassung> fassungen = teil.sprachfassungen();
    for (Sprachfassung f : fassungen) {
      Tab tab = new Tab(f.sprache().name());
      tab.getElement().setProperty("_sprache", f.sprache().name());
      tabs.add(tab);
    }
    if (!fassungen.isEmpty()) {
      tabs.addSelectedChangeListener(e -> {
        int idx = tabs.getSelectedIndex();
        if (idx >= 0 && idx < teil.sprachfassungen().size()) {
          showEditor(editor, teil.sprachfassungen().get(idx));
        }
      });
      body.add(tabs);
    }

    body.add(addLanguageBar());
    body.add(editor);

    if (!fassungen.isEmpty()) {
      showEditor(editor, fassungen.get(0));
    } else {
      editor.add(new EmptyState(VaadinIcon.PLUS,
          tr("fassung.none.title", "No language version yet"),
          tr("fassung.none.body", "Add a language above to start a manuscript.")));
    }
  }

  private HorizontalLayout addLanguageBar() {
    HorizontalLayout bar = new HorizontalLayout();
    bar.getStyle().set("gap", "var(--lumo-space-s)");
    Arrays.stream(Sprache.values())
        .filter(s -> teil.fassung(s).isEmpty())
        .forEach(s -> {
          Button add = new Button(tr("fassung.add", "+ {0}", s.name()), e -> {
            teil.addSprachfassung(s);
            repo.persist();
            render();
          });
          add.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
          bar.add(add);
        });
    return bar;
  }

  private void showEditor(Div editor, Sprachfassung fassung) {
    editor.removeAll();

    TextField manuskript = new TextField(tr("fassung.manuscript", "Manuscript reference"));
    manuskript.setWidthFull();
    manuskript.setValue(fassung.manuskript() == null ? "" : fassung.manuskript());
    manuskript.addValueChangeListener(e -> {
      fassung.setManuskript(e.getValue());
      repo.persist();
    });

    IntegerField zeichen = new IntegerField(tr("fassung.chars", "Planned characters"));
    zeichen.setMin(0);
    zeichen.setValue(fassung.geplanteZeichen());
    zeichen.addValueChangeListener(e -> {
      fassung.setGeplanteZeichen(e.getValue() == null ? 0 : e.getValue());
      repo.persist();
    });

    HorizontalLayout fields = new HorizontalLayout(manuskript, zeichen);
    fields.setWidthFull();
    fields.setFlexGrow(1, manuskript);
    editor.add(fields);

    Grid<Veroeffentlichung> grid = new Grid<>(Veroeffentlichung.class, false);
    grid.addColumn(v -> v.ort().name()).setHeader(tr("fassung.col.place", "Place")).setFlexGrow(1);
    grid.addComponentColumn(v -> PublicationUi.vertrieb(v.akquisestatus()))
        .setHeader(tr("fassung.col.acquisition", "Acquisition")).setAutoWidth(true);
    grid.addComponentColumn(v -> PublicationUi.herstellung(v.herstellungsstatus()))
        .setHeader(tr("fassung.col.production", "Production")).setAutoWidth(true);
    grid.addColumn(v -> v.datum() == null ? "" : v.datum().toString())
        .setHeader(tr("fassung.col.date", "Date")).setAutoWidth(true);
    grid.addComponentColumn(v -> {
      Button open = new Button(tr("fassung.open", "Open"),
          e -> UI.getCurrent().navigate("veroeffentlichung/" + v.id()));
      open.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
      return open;
    }).setAutoWidth(true);
    grid.setItems(fassung.veroeffentlichungen());
    grid.setAllRowsVisible(true);
    editor.add(grid);

    editor.add(planBar(fassung));
  }

  private HorizontalLayout planBar(Sprachfassung fassung) {
    ComboBox<Publikationsort> ortWahl = new ComboBox<>(tr("fassung.plan.place", "Place"));
    List<Publikationsort> zulaessig = repo.orteFuer(fassung.sprache());
    ortWahl.setItems(zulaessig);
    ortWahl.setItemLabelGenerator(Publikationsort::name);
    ortWahl.setPlaceholder(tr("fassung.plan.hint", "Only places that support {0}", fassung.sprache().name()));
    ortWahl.setWidth("320px");

    Button plan = new Button(tr("fassung.plan", "Plan publication"), e -> {
      Publikationsort ort = ortWahl.getValue();
      if (ort == null) {
        ortWahl.setInvalid(true);
        return;
      }
      fassung.planeVeroeffentlichung(ort);
      repo.persist();
      render();
    });
    plan.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    HorizontalLayout bar = new HorizontalLayout(ortWahl, plan);
    bar.setDefaultVerticalComponentAlignment(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.END);
    bar.getStyle().set("margin-top", "var(--lumo-space-m)");
    return bar;
  }
}
