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
import com.svenruppert.publications.model.Datenwurzel;
import org.eclipse.store.storage.embedded.types.EmbeddedStorage;
import org.eclipse.store.storage.embedded.types.EmbeddedStorageManager;

import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Produktions-Default der {@link PublicationsPersistence}. Hält den
 * {@link Datenwurzel}-Graph in einer <strong>eigenen</strong> Eclipse-Store-
 * Instanz — getrennt von der jSentinel-Framework-Storage und der
 * User-Directory-Storage, aus denselben Gründen (single-rooted Framework-Root,
 * unabhängige Migration/Backup/Reset).
 *
 * <p>Default-Verzeichnis: {@code ./data/app/publications}. Lazy geöffnet beim
 * ersten {@link #load()}, geschlossen über Shutdown-Hook + explizites
 * {@link #close()}. {@link #save} nutzt einen Eager-Storer, der den gesamten
 * erreichbaren Objektgraph tief persistiert — für den MVP-Datenumfang einfach
 * und korrekt, ohne manuelles Dirty-Tracking.
 */
public final class EclipseStorePublicationsPersistence
    implements PublicationsPersistence, HasLogger {

  /** Reads {@link AppStoragePaths#PROPERTY} so tests can redirect the base. */
  public static final Path DEFAULT_STORAGE_DIR = AppStoragePaths.publicationsDir();

  private final Path storageDir;
  private final AtomicBoolean closed = new AtomicBoolean();
  private volatile EmbeddedStorageManager manager;
  private volatile Datenwurzel root;

  public EclipseStorePublicationsPersistence() {
    this(DEFAULT_STORAGE_DIR);
  }

  public EclipseStorePublicationsPersistence(Path storageDir) {
    this.storageDir = Objects.requireNonNull(storageDir, "storageDir");
  }

  @Override
  public synchronized Datenwurzel load() {
    ensureOpen();
    return root;
  }

  @Override
  public synchronized void save(Datenwurzel wurzel) {
    ensureOpen();
    Objects.requireNonNull(wurzel, "wurzel");
    if (wurzel != root) {
      root = wurzel;
      manager.setRoot(root);
      manager.storeRoot();
    }
    var storer = manager.createEagerStorer();
    storer.store(root);
    storer.commit();
    logger().debug("EclipseStorePublicationsPersistence: persisted {} issues, {} orte to {}",
        root.issues().size(), root.publikationsorte().size(), storageDir);
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
    Datenwurzel initial = new Datenwurzel();
    manager = EmbeddedStorage.start(initial, storageDir);
    Object loaded = manager.root();
    if (loaded instanceof Datenwurzel existing) {
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
              + " — expected " + Datenwurzel.class.getName());
    }
    Runtime.getRuntime().addShutdownHook(new Thread(this::close,
        "app-publications-storage-shutdown"));
  }
}
