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

package com.svenruppert.publications.importetl;

import java.util.Map;

/**
 * Result of a transform-and-load run.
 *
 * @param created            newly created issues
 * @param updated            updated issues (an idempotent re-import changes nothing)
 * @param skipped            already-present issues (recognized by origin)
 * @param statusDistribution the mapping of the conflated ClickUp status onto the
 *                           disentangled dimension, counted (for the console)
 */
public record ImportReport(
    int created,
    int updated,
    int skipped,
    Map<String, Integer> statusDistribution) {

  public int total() {
    return created + updated + skipped;
  }
}
