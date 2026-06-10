/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.soloader;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

public final class FileLockManager {

  private static final ConcurrentHashMap<String, CountDownLatch> sLatches =
      new ConcurrentHashMap<>();

  private FileLockManager() {}

  public static FileLocker lock(File lockFile) throws IOException {
    String key = lockFile.getCanonicalPath();
    CountDownLatch myLatch = new CountDownLatch(1);

    while (true) {
      CountDownLatch existing = sLatches.putIfAbsent(key, myLatch);
      if (existing == null) {
        break;
      }
      LogUtil.w("soloader.FileLockManager", "Waiting for in-process lock for " + key);
      try {
        existing.await();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IOException("Interrupted while waiting for in-process lock for " + key, e);
      }
    }

    Runnable cleanup = new CleanupRunnable(key, myLatch);

    try {
      return FileLocker.lock(lockFile, cleanup);
    } catch (Throwable t) {
      cleanup.run();
      throw t;
    }
  }

  private static final class CleanupRunnable implements Runnable {
    private final String mKey;
    private final CountDownLatch mLatch;

    CleanupRunnable(String key, CountDownLatch latch) {
      mKey = key;
      mLatch = latch;
    }

    @Override
    public void run() {
      sLatches.remove(mKey, mLatch);
      mLatch.countDown();
    }
  }
}
