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

package com.svenruppert.flow.views.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;

/**
 * Factory for the back button used on the deep sub-pages (language-version
 * editor, publication view, history), so the user can step back to the previous
 * sub-item without going through the main navigation. A left-arrow tertiary
 * button that navigates to a target route.
 */
public final class BackButton {

  private BackButton() {
  }

  /** A back button labelled {@code label} that navigates to {@code route}. */
  public static Button to(String label, String route) {
    Button button = new Button(label, VaadinIcon.ARROW_LEFT.create(),
        e -> UI.getCurrent().navigate(route));
    button.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
    return button;
  }
}
