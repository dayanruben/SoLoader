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

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileLock;
import javax.annotation.Nullable;

public final class FileLocker implements Closeable {

  private final FileOutputStream mLockFileOutputStream;
  private final @Nullable FileLock mLock;
  private final @Nullable Runnable mOnClose;

  public static FileLocker lock(File lockFile) throws IOException {
    return new FileLocker(lockFile, null);
  }

  public static FileLocker lock(File lockFile, @Nullable Runnable onClose) throws IOException {
    return new FileLocker(lockFile, onClose);
  }

  private FileLocker(File lockFile) throws IOException {
    this(lockFile, null);
  }

  private FileLocker(File lockFile, @Nullable Runnable onClose) throws IOException {
    mLockFileOutputStream = new FileOutputStream(lockFile);
    FileLock lock = null;
    try {
      lock = mLockFileOutputStream.getChannel().lock();
    } finally {
      if (lock == null) {
        mLockFileOutputStream.close();
      }
    }

    mLock = lock;
    mOnClose = onClose;
  }

  @Override
  public void close() throws IOException {
    try {
      if (mLock != null) {
        mLock.release();
      }
    } finally {
      try {
        mLockFileOutputStream.close();
      } finally {
        if (mOnClose != null) {
          mOnClose.run();
        }
      }
    }
  }
}
