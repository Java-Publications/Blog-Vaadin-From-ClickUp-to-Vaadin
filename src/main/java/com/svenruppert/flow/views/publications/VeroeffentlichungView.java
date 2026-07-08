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
import com.svenruppert.flow.views.ui.EmptyState;
import com.svenruppert.flow.views.ui.PageHeader;
import com.svenruppert.flow.views.ui.TemplateBrand;
import com.svenruppert.jsentinel.authorization.api.SubjectStores;
import com.svenruppert.publications.model.Publikationsort;
import com.svenruppert.publications.model.Statuswechsel;
import com.svenruppert.publications.model.Veroeffentlichung;
import com.svenruppert.publications.model.Veroeffentlichungsstatus;
import com.svenruppert.publications.model.Vertriebsstatus;
import com.svenruppert.publications.persistence.PublicationsProvider;
import com.svenruppert.publications.persistence.PublicationsRepository;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * V4 — Veröffentlichungssicht, der dramaturgische Kern. Stellt die beiden
 * orthogonalen Lebenszyklen einer {@link Veroeffentlichung} in zwei
 * gleichwertigen Spalten nebeneinander: links die Akquise
 * ({@link Vertriebsstatus}), rechts die Herstellung
 * ({@link Veroeffentlichungsstatus}). Diese visuelle Zweiteilung ist selbst die
 * Botschaft der Entflechtung. Ortswahl der Zweitverwertung ist Sprachregel-
 * gefiltert. Bediente Prozesse: P0008–P0013.
 */
@Route(value = VeroeffentlichungView.NAV, layout = MainLayout.class)
@VisibleFor(AuthorizationRole.USER)
public class VeroeffentlichungView extends Composite<VerticalLayout>
    implements HasUrlParameter<String>, I18nSupport {

  public static final String NAV = "veroeffentlichung";

  private final transient PublicationsRepository repo = PublicationsProvider.repository();
  private final Div body = new Div();
  private transient Veroeffentlichung v;

  public VeroeffentlichungView() {
    VerticalLayout root = getContent();
    root.setSizeFull();
    root.getStyle().set("gap", "var(--lumo-space-m)");
    body.setSizeFull();
    root.add(body);
  }

  @Override
  public void setParameter(BeforeEvent event, String id) {
    this.v = parse(id).flatMap(repo::findVeroeffentlichung).orElse(null);
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
    if (v == null) {
      body.add(new EmptyState(VaadinIcon.NEWSPAPER,
          tr("pub.notfound.title", "Publication not found"),
          tr("pub.notfound.body", "Open a publication from a language version.")));
      return;
    }

    body.add(new PageHeader(
        tr("pub.heading", "Publication"),
        tr("pub.subtitle", "Two orthogonal lifecycles side by side.")));

    body.add(header());

    FlexLayout columns = new FlexLayout();
    columns.setWidthFull();
    columns.getStyle().set("gap", "var(--lumo-space-l)");
    columns.setFlexWrap(FlexLayout.FlexWrap.WRAP);
    columns.add(akquiseColumn(), herstellungColumn());
    body.add(columns);

    body.add(zweitverwertung());
  }

  private Div header() {
    Div card = new Div();
    card.addClassName(TemplateBrand.CSS_CARD);
    card.getStyle().set("padding", "var(--lumo-space-m)");

    Span fassung = new Span(tr("pub.version", "Version") + ": "
        + v.fassung().sprache().name() + " · " + v.fassung().geplanteZeichen() + " "
        + tr("pub.chars", "chars"));
    Span ort = new Span(tr("pub.place", "Place") + ": " + v.ort().name());
    ort.getStyle().set("margin-left", "var(--lumo-space-l)");

    DatePicker datum = new DatePicker(tr("pub.date", "Date"));
    datum.setValue(v.datum());
    datum.addValueChangeListener(e -> {
      v.setDatum(e.getValue());
      repo.persist();
    });

    TextField verweis = new TextField(tr("pub.link", "Link"));
    verweis.setValue(v.verweis() == null ? "" : v.verweis());
    verweis.addValueChangeListener(e -> {
      v.setVerweis(e.getValue());
      repo.persist();
    });

    TextField auftraggeber = new TextField(tr("pub.client", "Client"));
    auftraggeber.setValue(v.auftraggeber() == null ? "" : v.auftraggeber());
    auftraggeber.addValueChangeListener(e -> {
      v.setAuftraggeber(e.getValue() == null || e.getValue().isBlank() ? null : e.getValue());
      repo.persist();
    });

    HorizontalLayout meta = new HorizontalLayout(fassung, ort);
    HorizontalLayout fields = new HorizontalLayout(datum, verweis, auftraggeber);
    fields.setWidthFull();
    fields.setFlexGrow(1, verweis);
    card.add(meta, fields);
    return card;
  }

  private Div akquiseColumn() {
    Select<Vertriebsstatus> select = new Select<>();
    select.setItems(Vertriebsstatus.values());
    select.setValue(v.akquisestatus());
    select.addValueChangeListener(e -> {
      if (e.getValue() != null && e.getValue() != v.akquisestatus()) {
        v.wechsleVertrieb(e.getValue(), akteur());
        repo.persist();
        render();
      }
    });
    String last = v.akquise().ereignisse().isEmpty()
        ? tr("pub.nohistory", "no change yet")
        : format(v.akquise().ereignisse().get(v.akquise().ereignisse().size() - 1));
    return column(tr("pub.acquisition", "ACQUISITION"),
        tr("pub.acquisition.sub", "Sales — reduced, no billing"),
        PublicationUi.vertrieb(v.akquisestatus()), last, select,
        "verlauf/akquise/" + v.id());
  }

  private Div herstellungColumn() {
    Select<Veroeffentlichungsstatus> select = new Select<>();
    select.setItems(Veroeffentlichungsstatus.values());
    select.setValue(v.herstellungsstatus());
    select.addValueChangeListener(e -> {
      if (e.getValue() != null && e.getValue() != v.herstellungsstatus()) {
        v.wechsleStatus(e.getValue(), akteur());
        repo.persist();
        render();
      }
    });
    String last = v.herstellung().ereignisse().isEmpty()
        ? tr("pub.nohistory", "no change yet")
        : format(v.herstellung().ereignisse().get(v.herstellung().ereignisse().size() - 1));
    return column(tr("pub.production", "PRODUCTION"),
        tr("pub.production.sub", "From planning to release"),
        PublicationUi.herstellung(v.herstellungsstatus()), last, select,
        "verlauf/herstellung/" + v.id());
  }

  private Div column(String title, String subtitle, Span bigBadge, String lastLine,
                     Select<?> select, String verlaufPath) {
    Div card = new Div();
    card.addClassName(TemplateBrand.CSS_CARD);
    card.getStyle().set("padding", "var(--lumo-space-m)");
    card.getStyle().set("flex", "1 1 320px");

    Span head = new Span(title);
    head.getStyle().set("font-weight", "600");
    head.getStyle().set("letter-spacing", "0.06em");
    Span sub = new Span(subtitle);
    sub.getStyle().set("color", "var(--lumo-secondary-text-color)");
    sub.getStyle().set("font-size", "var(--lumo-font-size-s)");
    sub.getStyle().set("display", "block");

    bigBadge.getStyle().set("font-size", "var(--lumo-font-size-l)");
    Span last = new Span(lastLine);
    last.getStyle().set("color", "var(--lumo-secondary-text-color)");
    last.getStyle().set("font-size", "var(--lumo-font-size-s)");
    last.getStyle().set("display", "block");

    Button verlauf = new Button(tr("pub.history", "History ›"),
        e -> UI.getCurrent().navigate(verlaufPath));
    verlauf.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

    select.setLabel(tr("pub.advance", "Advance"));

    card.add(head, sub, new Div(bigBadge), last, select, verlauf);
    return card;
  }

  private Div zweitverwertung() {
    Div card = new Div();
    card.getStyle().set("margin-top", "var(--lumo-space-m)");

    List<Publikationsort> weitere = repo.orteFuer(v.sprache()).stream()
        .filter(o -> !o.id().equals(v.ort().id()))
        .toList();

    ComboBox<Publikationsort> ortWahl = new ComboBox<>(tr("pub.reuse", "Second use"));
    ortWahl.setItems(weitere);
    ortWahl.setItemLabelGenerator(Publikationsort::name);
    ortWahl.setPlaceholder(tr("pub.reuse.hint", "Another place supporting {0}", v.sprache().name()));
    ortWahl.setWidth("320px");

    Button anlegen = new Button(tr("pub.reuse.action", "Create second use"), e -> {
      Publikationsort ort = ortWahl.getValue();
      if (ort == null) {
        ortWahl.setInvalid(true);
        return;
      }
      v.fassung().planeVeroeffentlichung(ort);
      repo.persist();
      UI.getCurrent().navigate(NAV + "/" + v.id());
    });
    anlegen.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

    HorizontalLayout bar = new HorizontalLayout(ortWahl, anlegen);
    bar.setDefaultVerticalComponentAlignment(
        com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.END);
    card.add(bar);
    return card;
  }

  private static String format(Statuswechsel<?> w) {
    return w.von() + " → " + w.nach() + " · " + (w.akteur() == null ? "—" : w.akteur());
  }

  private static String akteur() {
    return SubjectStores.subjectStore().currentSubject(AppUser.class)
        .map(AppUser::name).orElse("system");
  }
}
