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
import com.svenruppert.flow.views.ui.BackButton;
import com.svenruppert.flow.views.ui.EmptyState;
import com.svenruppert.flow.views.ui.PageHeader;
import com.svenruppert.publications.model.Language;
import com.svenruppert.publications.model.LanguageVersion;
import com.svenruppert.publications.model.Part;
import com.svenruppert.publications.model.Publication;
import com.svenruppert.publications.model.PublicationPlace;
import com.svenruppert.publications.persistence.PublicationsProvider;
import com.svenruppert.publications.persistence.PublicationsRepository;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
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
import java.util.Optional;
import java.util.UUID;

/**
 * V2 — Language-version editor. For a {@link Part}, the manuscript plus the
 * planned character count are maintained per {@link Language}; at the same time
 * this view is the jump-off point for planning a {@link Publication}
 * (language-rule filtered).
 *
 * <p>First parametrised route in the project: {@code teil/<uuid>}. Served
 * processes: P0005, P0006; jump-off to P0008/P0012.
 */
@Route(value = LanguageVersionView.NAV, layout = MainLayout.class)
@VisibleFor(AuthorizationRole.USER)
public class LanguageVersionView extends Composite<VerticalLayout>
    implements HasUrlParameter<String>, I18nSupport {

  public static final String NAV = "part";

  private static final long serialVersionUID = 1L;

  private final transient PublicationsRepository repo = PublicationsProvider.repository();
  private final Div body = new Div();

  private transient Part part;

  public LanguageVersionView() {
    VerticalLayout root = getContent();
    root.setSizeFull();
    root.getStyle().set("gap", "var(--lumo-space-m)");
    body.setSizeFull();
    root.add(body);
  }

  @Override
  public void setParameter(BeforeEvent event, String partId) {
    this.part = parse(partId).flatMap(repo::findPart).orElse(null);
    render();
  }

  private static Optional<UUID> parse(String id) {
    try {
      return Optional.of(UUID.fromString(id));
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }

  private void render() {
    body.removeAll();
    if (part == null) {
      body.add(new EmptyState(VaadinIcon.FILE_TEXT_O,
          tr("fassung.notfound.title", "Part not found"),
          tr("fassung.notfound.body", "Open a part from the topic workspace.")));
      return;
    }

    body.add(BackButton.to(tr("fassung.back", "Back to topic workspace"), TopicsView.NAV));

    String prefix = part.issue() != null ? part.issue().title() : "";
    body.add(new PageHeader(
        tr("fassung.heading", "Language versions"),
        prefix + " · " + tr("fassung.part", "Part {0}", part.position())));

    Tabs tabs = new Tabs();
    Div editor = new Div();
    editor.setWidthFull();

    List<LanguageVersion> versions = part.languageVersions();
    for (LanguageVersion v : versions) {
      Tab tab = new Tab(v.language().name());
      tabs.add(tab);
    }
    if (!versions.isEmpty()) {
      tabs.addSelectedChangeListener(e -> {
        int idx = tabs.getSelectedIndex();
        if (idx >= 0 && idx < part.languageVersions().size()) {
          showEditor(editor, part.languageVersions().get(idx));
        }
      });
      body.add(tabs);
    }

    body.add(addLanguageBar());
    body.add(editor);

    if (!versions.isEmpty()) {
      showEditor(editor, versions.get(0));
    } else {
      editor.add(new EmptyState(VaadinIcon.PLUS,
          tr("fassung.none.title", "No language version yet"),
          tr("fassung.none.body", "Add a language above to start a manuscript.")));
    }
  }

  private HorizontalLayout addLanguageBar() {
    HorizontalLayout bar = new HorizontalLayout();
    bar.getStyle().set("gap", "var(--lumo-space-s)");
    Arrays.stream(Language.values())
        .filter(l -> part.versionFor(l).isEmpty())
        .forEach(l -> {
          Button add = new Button(tr("fassung.add", "+ {0}", l.name()), e -> {
            part.addLanguageVersion(l);
            repo.persist();
            render();
          });
          add.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
          bar.add(add);
        });
    return bar;
  }

  private void showEditor(Div editor, LanguageVersion version) {
    editor.removeAll();

    TextField manuscript = new TextField(tr("fassung.manuscript", "Manuscript reference"));
    manuscript.setWidthFull();
    manuscript.setValue(version.manuscript() == null ? "" : version.manuscript());
    manuscript.addValueChangeListener(e -> {
      version.setManuscript(e.getValue());
      repo.persist();
    });

    IntegerField characters = new IntegerField(tr("fassung.chars", "Planned characters"));
    characters.setMin(0);
    characters.setValue(version.plannedCharacters());
    characters.addValueChangeListener(e -> {
      version.setPlannedCharacters(e.getValue() == null ? 0 : e.getValue());
      repo.persist();
    });

    HorizontalLayout fields = new HorizontalLayout(manuscript, characters);
    fields.setWidthFull();
    fields.setFlexGrow(1, manuscript);
    editor.add(fields);

    // Next-step guidance (Q): after the manuscript, the workflow continues by
    // planning where this version gets published.
    H4 pubHeading = new H4(tr("fassung.pub.heading", "Publications of this version"));
    pubHeading.getStyle().set("margin", "var(--lumo-space-m) 0 0");
    Span pubHint = new Span(tr("fassung.pub.hint",
        "Next step: plan where this version gets published. Each publication then "
            + "tracks its own acquisition and production lifecycle."));
    pubHint.getStyle().set("color", "var(--lumo-secondary-text-color)");
    pubHint.getStyle().set("font-size", "var(--lumo-font-size-s)");
    pubHint.getStyle().set("display", "block");
    editor.add(pubHeading, pubHint);

    if (version.publications().isEmpty()) {
      editor.add(new EmptyState(VaadinIcon.NEWSPAPER,
          tr("fassung.pub.empty.title", "Not published anywhere yet"),
          tr("fassung.pub.empty.body", "Pick a place below and plan the first publication.")));
    } else {
      Grid<Publication> grid = new Grid<>(Publication.class, false);
      grid.addColumn(v -> v.place().name()).setHeader(tr("fassung.col.place", "Place")).setFlexGrow(1);
      grid.addComponentColumn(v -> PublicationUi.acquisition(v.acquisitionStatus()))
          .setHeader(tr("fassung.col.acquisition", "Acquisition")).setAutoWidth(true);
      grid.addComponentColumn(v -> PublicationUi.production(v.productionStatus()))
          .setHeader(tr("fassung.col.production", "Production")).setAutoWidth(true);
      grid.addColumn(v -> v.date() == null ? "" : v.date().toString())
          .setHeader(tr("fassung.col.date", "Date")).setAutoWidth(true);
      grid.addComponentColumn(v -> {
        Button open = new Button(tr("fassung.open", "Open"),
            e -> UI.getCurrent().navigate("publication/" + v.id()));
        open.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        return open;
      }).setAutoWidth(true);
      grid.setItems(version.publications());
      grid.setAllRowsVisible(true);
      editor.add(grid);
    }

    editor.add(planBar(version));
  }

  private HorizontalLayout planBar(LanguageVersion version) {
    ComboBox<PublicationPlace> placeChoice = new ComboBox<>(tr("fassung.plan.place", "Place"));
    List<PublicationPlace> admissible = repo.placesFor(version.language());
    placeChoice.setItems(admissible);
    placeChoice.setItemLabelGenerator(PublicationPlace::name);
    placeChoice.setPlaceholder(tr("fassung.plan.hint", "Only places that support {0}", version.language().name()));
    placeChoice.setWidth("320px");

    Button plan = new Button(tr("fassung.plan", "Plan publication"), e -> {
      PublicationPlace place = placeChoice.getValue();
      if (place == null) {
        placeChoice.setInvalid(true);
        return;
      }
      version.planPublication(place);
      repo.persist();
      render();
    });
    plan.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    HorizontalLayout bar = new HorizontalLayout(placeChoice, plan);
    bar.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.END);
    bar.getStyle().set("margin-top", "var(--lumo-space-m)");
    return bar;
  }
}
