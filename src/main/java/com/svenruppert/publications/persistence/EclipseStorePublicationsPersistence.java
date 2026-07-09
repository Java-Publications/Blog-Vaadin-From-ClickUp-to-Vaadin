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

package com.svenruppert.publications.persistence;

import com.svenruppert.dependencies.core.logger.HasLogger;
import com.svenruppert.flow.security.storage.AppStoragePaths;
import com.svenruppert.publications.model.DataRoot;
import org.eclipse.store.storage.embedded.types.EmbeddedStorage;
import org.eclipse.store.storage.embedded.types.EmbeddedStorageManager;

import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Production default of {@link PublicationsPersistence}. Holds the
 * {@link DataRoot} graph in its <strong>own</strong> Eclipse-Store instance —
 * separate from the jSentinel framework storage and the user-directory storage,
 * for the same reasons (single-rooted framework root, independent migration/
 * backup/reset).
 *
 * <p>Default directory: {@code ./data/app/publications}. Opened lazily on first
 * {@link #load()}, closed via a shutdown hook plus an explicit {@link #close()}.
 * {@link #save} uses an eager storer that deeply persists the whole reachable
 * object graph — simple and correct for the MVP data volume, without manual
 * dirty tracking.
 */
public final class EclipseStorePublicationsPersistence
    implements PublicationsPersistence, HasLogger {

  /** Reads {@link AppStoragePaths#PROPERTY} so tests can redirect the base. */
  public static final Path DEFAULT_STORAGE_DIR = AppStoragePaths.publicationsDir();

  private final Path storageDir;
  private final AtomicBoolean closed = new AtomicBoolean();
  private volatile EmbeddedStorageManager manager;
  private volatile DataRoot root;

  public EclipseStorePublicationsPersistence() {
    this(DEFAULT_STORAGE_DIR);
  }

  public EclipseStorePublicationsPersistence(Path storageDir) {
    this.storageDir = Objects.requireNonNull(storageDir, "storageDir");
  }

  @Override
  public synchronized DataRoot load() {
    ensureOpen();
    return root;
  }

  @Override
  public synchronized void save(DataRoot root) {
    ensureOpen();
    Objects.requireNonNull(root, "root");
    if (root != this.root) {
      this.root = root;
      manager.setRoot(root);
      manager.storeRoot();
    }
    var storer = manager.createEagerStorer();
    storer.store(this.root);
    storer.commit();
    logger().debug("EclipseStorePublicationsPersistence: persisted {} issues, {} places to {}",
        this.root.issues().size(), this.root.publicationPlaces().size(), storageDir);
  }

  @Override
  public synchronized void close() {
    if (!closed.compareAndSet(false, true)) return;
    if (manager != null) {
      try {
        manager.shutdown();
        logger().info("EclipseStorePublicationsPersistence: storage at {} closed", storageDir);
      } catch (RuntimeException e) {
        logger().warn("EclipseStorePublicationsPersistence: shutdown failed for {}", storageDir, e);
      }
    }
  }

  private void ensureOpen() {
    if (manager != null) return;
    DataRoot initial = new DataRoot();
    manager = EmbeddedStorage.start(initial, storageDir);
    Object loaded = manager.root();
    if (loaded instanceof DataRoot existing) {
      root = existing;
      logger().info("EclipseStorePublicationsPersistence: opened {} ({} issues present)",
          storageDir, root.issues().size());
    } else if (loaded == null) {
      root = initial;
      manager.setRoot(root);
      manager.storeRoot();
      logger().info("EclipseStorePublicationsPersistence: bootstrapped fresh storage at {}", storageDir);
    } else {
      manager.shutdown();
      throw new IllegalStateException(
          "Unexpected root type in " + storageDir + ": " + loaded.getClass().getName()
              + " — expected " + DataRoot.class.getName());
    }
    Runtime.getRuntime().addShutdownHook(new Thread(this::close,
        "app-publications-storage-shutdown"));
  }
}
