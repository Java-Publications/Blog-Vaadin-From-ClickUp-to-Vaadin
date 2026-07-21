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
import com.svenruppert.flow.views.ui.BackButton;
import com.svenruppert.flow.views.ui.EmptyState;
import com.svenruppert.flow.views.ui.PageHeader;
import com.svenruppert.flow.views.ui.TemplateBrand;
import com.svenruppert.jsentinel.authorization.api.SubjectStores;
import com.svenruppert.publications.model.AcquisitionStatus;
import com.svenruppert.publications.model.Client;
import com.svenruppert.publications.model.Part;
import com.svenruppert.publications.model.ProductionStatus;
import com.svenruppert.publications.model.Publication;
import com.svenruppert.publications.model.PublicationPlace;
import com.svenruppert.publications.model.StatusChange;
import com.svenruppert.publications.persistence.PublicationsProvider;
import com.svenruppert.publications.persistence.PublicationsRepository;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
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
 * V4 — Publication view, the dramaturgical core. Places the two orthogonal
 * lifecycles of a {@link Publication} side by side in two equal columns: on the
 * left the acquisition ({@link AcquisitionStatus}), on the right the production
 * ({@link ProductionStatus}). This visual split is itself the message of the
 * disentanglement. The place choice for the second use is language-rule filtered.
 * Served processes: P0008–P0013.
 */
@Route(value = PublicationView.NAV, layout = MainLayout.class)
@VisibleFor(AuthorizationRole.USER)
public class PublicationView extends Composite<VerticalLayout>
    implements HasUrlParameter<String>, I18nSupport {

  public static final String NAV = "publication";

  private static final long serialVersionUID = 1L;

  private final transient PublicationsRepository repo = PublicationsProvider.repository();
  private final Div body = new Div();
  private transient Publication publication;

  public PublicationView() {
    VerticalLayout root = getContent();
    root.setSizeFull();
    root.getStyle().set("gap", "var(--lumo-space-m)");
    body.setSizeFull();
    root.add(body);
  }

  @Override
  public void setParameter(BeforeEvent event, String id) {
    this.publication = parse(id).flatMap(repo::findPublication).orElse(null);
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
    if (publication == null) {
      body.add(new EmptyState(VaadinIcon.NEWSPAPER,
          tr("pub.notfound.title", "Publication not found"),
          tr("pub.notfound.body", "Open a publication from a language version.")));
      return;
    }

    Part owningPart = repo.partOf(publication.version()).orElse(null);
    if (owningPart != null) {
      body.add(BackButton.to(tr("pub.back", "Back to the language version"),
          "part/" + owningPart.id()));
    }

    body.add(new PageHeader(
        tr("pub.heading", "Publication"),
        tr("pub.subtitle", "Two orthogonal lifecycles side by side.")));

    body.add(header(owningPart));

    FlexLayout columns = new FlexLayout();
    columns.setWidthFull();
    columns.getStyle().set("gap", "var(--lumo-space-l)");
    columns.setFlexWrap(FlexLayout.FlexWrap.WRAP);
    columns.add(acquisitionColumn(), productionColumn());
    body.add(columns);

    body.add(secondUse());
  }

  private Div header(Part owningPart) {
    Div card = new Div();
    card.addClassName(TemplateBrand.CSS_CARD);
    card.getStyle().set("padding", "var(--lumo-space-m)");

    // Which blog post this publication belongs to (R).
    Span blogPost = new Span(tr("pub.blogpost", "Blog post: {0}",
        PublicationUi.blogPost(owningPart)));
    blogPost.getStyle().set("font-weight", "600");
    blogPost.getStyle().set("display", "block");
    blogPost.getStyle().set("margin-bottom", "var(--lumo-space-xs)");

    Span version = new Span(tr("pub.version", "Version") + ": "
        + publication.version().language().name() + " · "
        + publication.version().plannedCharacters() + " " + tr("pub.chars", "chars"));
    Span place = new Span(tr("pub.place", "Place") + ": " + publication.place().name());
    place.getStyle().set("margin-left", "var(--lumo-space-l)");

    DatePicker date = new DatePicker(tr("pub.date", "Date"));
    date.setValue(publication.date());
    date.addValueChangeListener(e -> {
      publication.setDate(e.getValue());
      repo.persist();
    });

    TextField link = new TextField(tr("pub.link", "Link"));
    link.setValue(publication.link() == null ? "" : publication.link());
    link.addValueChangeListener(e -> {
      publication.setLink(e.getValue());
      repo.persist();
    });

    // Client picked from the maintainable master-data list, not typed freely (Y).
    ComboBox<Client> client = new ComboBox<>(tr("pub.client", "Client"));
    client.setItems(repo.clients());
    client.setItemLabelGenerator(Client::name);
    client.setClearButtonVisible(true);
    client.setPlaceholder(tr("pub.client.ph", "— none —"));
    repo.clients().stream()
        .filter(c -> c.name().equals(publication.client()))
        .findFirst().ifPresent(client::setValue);
    client.addValueChangeListener(e -> {
      publication.setClient(e.getValue() == null ? null : e.getValue().name());
      repo.persist();
    });

    HorizontalLayout meta = new HorizontalLayout(version, place);
    HorizontalLayout fields = new HorizontalLayout(date, link, client);
    fields.setWidthFull();
    fields.setFlexGrow(1, link);
    card.add(blogPost, meta, fields);
    return card;
  }

  private Div acquisitionColumn() {
    Select<AcquisitionStatus> select = new Select<>();
    select.setItems(AcquisitionStatus.values());
    select.setValue(publication.acquisitionStatus());
    select.addValueChangeListener(e -> {
      if (e.getValue() != null && e.getValue() != publication.acquisitionStatus()) {
        publication.changeAcquisitionStatus(e.getValue(), actor());
        repo.persist();
        render();
      }
    });
    String last = publication.acquisition().events().isEmpty()
        ? tr("pub.nohistory", "no change yet")
        : format(publication.acquisition().events().get(publication.acquisition().events().size() - 1));
    return column(tr("pub.acquisition", "ACQUISITION"),
        tr("pub.acquisition.sub", "Sales — reduced, no billing"),
        PublicationUi.acquisition(publication.acquisitionStatus()), last, select,
        "history/acquisition/" + publication.id());
  }

  private Div productionColumn() {
    Select<ProductionStatus> select = new Select<>();
    select.setItems(ProductionStatus.values());
    select.setValue(publication.productionStatus());
    select.addValueChangeListener(e -> {
      if (e.getValue() != null && e.getValue() != publication.productionStatus()) {
        publication.changeProductionStatus(e.getValue(), actor());
        repo.persist();
        render();
      }
    });
    String last = publication.production().events().isEmpty()
        ? tr("pub.nohistory", "no change yet")
        : format(publication.production().events().get(publication.production().events().size() - 1));
    return column(tr("pub.production", "PRODUCTION"),
        tr("pub.production.sub", "From planning to release"),
        PublicationUi.production(publication.productionStatus()), last, select,
        "history/production/" + publication.id());
  }

  private Div column(String title, String subtitle, Span bigBadge, String lastLine,
                     Select<?> select, String historyPath) {
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

    Button history = new Button(tr("pub.history", "History ›"),
        e -> UI.getCurrent().navigate(historyPath));
    history.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

    select.setLabel(tr("pub.advance", "Advance"));

    card.add(head, sub, new Div(bigBadge), last, select, history);
    return card;
  }

  private Div secondUse() {
    Div card = new Div();
    card.getStyle().set("margin-top", "var(--lumo-space-m)");

    // Show every use of this version, so it is visible that a second use exists
    // and where this version is published (Z).
    H4 usesHeading = new H4(tr("pub.uses.heading", "Uses of this version"));
    usesHeading.getStyle().set("margin", "0 0 var(--lumo-space-xs)");
    card.add(usesHeading);
    FlexLayout uses = new FlexLayout();
    uses.setFlexWrap(FlexLayout.FlexWrap.WRAP);
    uses.getStyle().set("gap", "var(--lumo-space-s)");
    for (Publication use : publication.version().publications()) {
      boolean current = use.id().equals(publication.id());
      Button chip = new Button(use.place().name()
          + (current ? " · " + tr("pub.uses.current", "current") : ""),
          e -> UI.getCurrent().navigate(NAV + "/" + use.id()));
      chip.addThemeVariants(current ? ButtonVariant.LUMO_PRIMARY : ButtonVariant.LUMO_TERTIARY,
          ButtonVariant.LUMO_SMALL);
      chip.setEnabled(!current);
      uses.add(chip);
    }
    card.add(uses);

    List<PublicationPlace> others = repo.placesFor(publication.language()).stream()
        .filter(o -> publication.version().publications().stream()
            .noneMatch(p -> p.place().id().equals(o.id())))
        .toList();

    ComboBox<PublicationPlace> placeChoice = new ComboBox<>(tr("pub.reuse", "Second use"));
    placeChoice.setItems(others);
    placeChoice.setItemLabelGenerator(PublicationPlace::name);
    placeChoice.setPlaceholder(others.isEmpty()
        ? tr("pub.reuse.none", "No further place supports {0}", publication.language().name())
        : tr("pub.reuse.hint", "Another place supporting {0}", publication.language().name()));
    placeChoice.setWidth("320px");
    placeChoice.setEnabled(!others.isEmpty());

    Button create = new Button(tr("pub.reuse.action", "Create second use"), e -> {
      PublicationPlace place = placeChoice.getValue();
      if (place == null) {
        placeChoice.setInvalid(true);
        return;
      }
      Publication created = publication.version().planPublication(place);
      repo.persist();
      Notification.show(tr("pub.reuse.created", "Second use at {0} created — opening it.", place.name()));
      UI.getCurrent().navigate(NAV + "/" + created.id());
    });
    create.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    create.setEnabled(!others.isEmpty());

    HorizontalLayout bar = new HorizontalLayout(placeChoice, create);
    bar.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.END);
    bar.getStyle().set("margin-top", "var(--lumo-space-m)");
    card.add(bar);
    return card;
  }

  private static String format(StatusChange<?> change) {
    return change.from() + " → " + change.to() + " · "
        + (change.actor() == null ? "—" : change.actor());
  }

  private static String actor() {
    return SubjectStores.subjectStore().currentSubject(AppUser.class)
        .map(AppUser::name).orElse("system");
  }
}
