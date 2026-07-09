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

import com.svenruppert.flow.i18n.I18n;
import com.svenruppert.flow.security.model.AppUser;
import com.svenruppert.jsentinel.authorization.api.SubjectStores;
import com.svenruppert.publications.model.EditorialState;
import com.svenruppert.publications.model.Issue;
import com.svenruppert.publications.model.Part;
import com.svenruppert.publications.persistence.PublicationsRepository;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.markdown.Markdown;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Reusable editor for a topic ({@link Issue}) and, optionally, one of its
 * {@link Part}s. Shared by the topic workspace (edit action) and the editorial
 * board (double-click on a card or table row). It is:
 * <ul>
 *   <li><b>resizable</b> and draggable, with a sensible default width;</li>
 *   <li>backed by a <b>markdown editor</b> for the original text, switchable
 *       between a raw <i>Syntax</i> {@link TextArea} and a rendered
 *       <i>Visual</i> {@link Markdown} preview;</li>
 *   <li>when opened for a part, it surfaces a prominent editorial-<b>status</b>
 *       control so changing the state is obvious and does not depend on
 *       drag&amp;drop.</li>
 * </ul>
 */
@SuppressFBWarnings(
    value = {"EI_EXPOSE_REP2", "SE_TRANSIENT_FIELD_NOT_RESTORED"},
    justification = "By design — this is a short-lived editor that edits the live Issue/Part it "
        + "was handed and persists through the given repository; it is created, shown and closed "
        + "within a single request and is never serialized and restored mid-edit, so the transient "
        + "domain references need no defensive copy or readObject.")
public final class TopicEditDialog extends Dialog {

  private static final long serialVersionUID = 1L;

  private final transient Issue issue;
  private final transient Part part;
  private final transient PublicationsRepository repo;
  private final transient Runnable onSaved;

  private final TextField titleField = new TextField(I18n.tr("themen.new.field", "Title"));
  private final TextArea source = new TextArea();
  private final RadioButtonGroup<EditorialState> statusGroup = new RadioButtonGroup<>();

  /**
   * @param issue   the topic being edited (required)
   * @param part    the part whose editorial status is edited, or {@code null} for
   *                a topic-only edit (no status control)
   * @param repo    persistence target
   * @param onSaved run after a successful save (e.g. re-render the caller)
   */
  public TopicEditDialog(Issue issue, Part part, PublicationsRepository repo, Runnable onSaved) {
    this.issue = issue;
    this.part = part;
    this.repo = repo;
    this.onSaved = onSaved;

    setHeaderTitle(I18n.tr("themen.edit.title", "Edit topic"));
    setResizable(true);
    setDraggable(true);
    setWidth("48em");
    setMinWidth("26em");

    VerticalLayout body = new VerticalLayout();
    body.setPadding(false);
    body.setSpacing(false);
    body.getStyle().set("gap", "var(--lumo-space-m)");

    titleField.setWidthFull();
    titleField.setValue(issue.title());
    body.add(titleField);

    if (part != null) {
      body.add(buildStatusPanel());
    }

    body.add(buildMarkdownEditor());
    add(body);

    Button save = new Button(I18n.tr("common.save", "Save"), e -> onSave());
    save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    Button cancel = new Button(I18n.tr("common.cancel", "Cancel"), e -> close());
    getFooter().add(cancel, save);
  }

  /** Prominent, always-visible editorial-status control (M). */
  private Div buildStatusPanel() {
    statusGroup.setLabel(I18n.tr("tafel.status", "Editorial status"));
    statusGroup.setItems(EditorialState.values());
    statusGroup.setItemLabelGenerator(EditorialState::name);
    statusGroup.setValue(part.editorialState());

    Div panel = new Div(statusGroup);
    panel.getStyle().set("padding", "var(--lumo-space-s) var(--lumo-space-m)");
    panel.getStyle().set("background", "var(--lumo-primary-color-10pct)");
    panel.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
    panel.getStyle().set("border", "1px solid var(--lumo-primary-color-50pct)");
    return panel;
  }

  /** Markdown editor: raw Syntax {@link TextArea} ↔ rendered Visual {@link Markdown}. */
  private Div buildMarkdownEditor() {
    Span label = new Span(I18n.tr("themen.detail.original", "Original text"));
    label.getStyle().set("font-weight", "600");

    source.setWidthFull();
    source.setMinHeight("14em");
    source.setValue(issue.description() == null ? "" : issue.description());

    Markdown preview = new Markdown(source.getValue());
    Div previewBox = new Div(preview);
    previewBox.setWidthFull();
    previewBox.getStyle().set("min-height", "14em");
    previewBox.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)");
    previewBox.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
    previewBox.getStyle().set("padding", "0 var(--lumo-space-s)");
    previewBox.getStyle().set("overflow-y", "auto");
    previewBox.setVisible(false);

    Tab syntaxTab = new Tab(I18n.tr("themen.edit.syntax", "Syntax"));
    Tab visualTab = new Tab(I18n.tr("themen.edit.visual", "Visual"));
    Tabs modes = new Tabs(syntaxTab, visualTab);
    modes.addSelectedChangeListener(e -> {
      boolean visual = modes.getSelectedTab() == visualTab;
      if (visual) {
        preview.setContent(source.getValue());
      }
      previewBox.setVisible(visual);
      source.setVisible(!visual);
    });

    Div editor = new Div(label, modes, source, previewBox);
    editor.setWidthFull();
    editor.getStyle().set("display", "flex");
    editor.getStyle().set("flex-direction", "column");
    editor.getStyle().set("gap", "var(--lumo-space-xs)");
    return editor;
  }

  private void onSave() {
    save(titleField.getValue(), source.getValue(),
        part != null ? statusGroup.getValue() : null);
  }

  /**
   * Applies the edited title, original text and (for a part) editorial status,
   * persists and runs the {@code onSaved} callback. Returns {@code false} without
   * changes when the title is blank. Public so the save path is unit-testable.
   */
  public boolean save(String title, String description, EditorialState status) {
    String value = title == null ? "" : title.strip();
    if (value.isEmpty()) {
      titleField.setInvalid(true);
      titleField.setErrorMessage(I18n.tr("themen.new.required", "Title required"));
      return false;
    }
    issue.setTitle(value);
    String body = description == null ? "" : description;
    issue.setDescription(body.isBlank() ? null : body);
    if (part != null && status != null && status != part.editorialState()) {
      part.changeState(status, actor());
    }
    repo.persist();
    close();
    onSaved.run();
    return true;
  }

  private static String actor() {
    return SubjectStores.subjectStore().currentSubject(AppUser.class)
        .map(AppUser::name).orElse("system");
  }
}
