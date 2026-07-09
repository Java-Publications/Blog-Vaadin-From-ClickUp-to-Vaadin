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

package junit.com.svenruppert.flow.security.storage;

import com.svenruppert.flow.security.storage.AppStoragePaths;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins every derived storage path against {@link AppStoragePaths#baseDir()} so
 * each path method's mutants are killed (in particular {@code publicationsDir()},
 * added for the publications domain).
 */
class AppStoragePathsTest {

  @Test
  void frameworkStorageIsBaseSlashJsentinel() {
    assertEquals(AppStoragePaths.baseDir().resolve("jsentinel"),
        AppStoragePaths.frameworkStorageDir());
  }

  @Test
  void userDirectoryIsBaseSlashAppUsers() {
    assertEquals(AppStoragePaths.baseDir().resolve("app").resolve("users"),
        AppStoragePaths.userDirectoryDir());
  }

  @Test
  void publicationsIsBaseSlashAppPublications() {
    assertEquals(AppStoragePaths.baseDir().resolve("app").resolve("publications"),
        AppStoragePaths.publicationsDir());
  }

  @Test
  void importIsBaseSlashAppImport() {
    assertEquals(AppStoragePaths.baseDir().resolve("app").resolve("import"),
        AppStoragePaths.importDir());
  }

  @Test
  void bootstrapTokenIsUnderFrameworkStorage() {
    assertEquals(AppStoragePaths.frameworkStorageDir().resolve("bootstrap.token"),
        AppStoragePaths.bootstrapTokenFile());
  }

  @Test
  void allAppPathsShareTheSameBase() {
    Path base = AppStoragePaths.baseDir();
    assertTrue(AppStoragePaths.publicationsDir().startsWith(base));
    assertTrue(AppStoragePaths.userDirectoryDir().startsWith(base));
    assertTrue(AppStoragePaths.frameworkStorageDir().startsWith(base));
    assertTrue(AppStoragePaths.importDir().startsWith(base));
    // publications and users are siblings under the same app/ directory
    assertEquals(AppStoragePaths.userDirectoryDir().getParent(),
        AppStoragePaths.publicationsDir().getParent());
  }
}
