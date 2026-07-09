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

import com.svenruppert.publications.model.EditorialState;
import com.svenruppert.publications.model.Issue;
import com.vaadin.flow.server.VaadinSession;

/**
 * Session-scoped filter shared by the global navbar controls (in
 * {@code MainLayout}) and the publications views (F6). It carries a title
 * substring and an optional {@link EditorialState}; the navbar search field and
 * state selector write it, {@code TopicsView} and {@code EditorialBoardView} read
 * it on render so the filter survives navigation between the views.
 *
 * <p>The instance lives as a {@link VaadinSession} attribute keyed by this class,
 * so every UI of the same session shares one filter.
 */
public final class PublicationsFilter {

  private String titleQuery = "";
  private EditorialState state;

  public String titleQuery() {
    return titleQuery;
  }

  public void setTitleQuery(String titleQuery) {
    this.titleQuery = titleQuery == null ? "" : titleQuery.strip();
  }

  /** The state to filter by, or {@code null} for "any state". */
  public EditorialState state() {
    return state;
  }

  public void setState(EditorialState state) {
    this.state = state;
  }

  public boolean isEmpty() {
    return titleQuery.isEmpty() && state == null;
  }

  /** {@code true} if {@code issue} matches the current title substring (case-insensitive). */
  public boolean matchesTitle(Issue issue) {
    if (titleQuery.isEmpty()) {
      return true;
    }
    return issue.title().toLowerCase().contains(titleQuery.toLowerCase());
  }

  /** {@code true} if no state is selected or {@code issue} has a part in that state. */
  public boolean matchesState(Issue issue) {
    if (state == null) {
      return true;
    }
    return issue.parts().stream().anyMatch(p -> p.editorialState() == state);
  }

  /**
   * The current session's filter, created and stored on first access. Returns a
   * detached, empty filter when there is no active session (e.g. outside a
   * request), so callers never deal with {@code null}.
   */
  public static PublicationsFilter current() {
    VaadinSession session = VaadinSession.getCurrent();
    if (session == null) {
      return new PublicationsFilter();
    }
    PublicationsFilter filter = session.getAttribute(PublicationsFilter.class);
    if (filter == null) {
      filter = new PublicationsFilter();
      session.setAttribute(PublicationsFilter.class, filter);
    }
    return filter;
  }
}
