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

/**
 * Represents an APK split that contains native libraries. There are two kinds:
 *
 * <ul>
 *   <li>{@link Installed} — an installed split whose name is known but whose path must be resolved
 *       from application info at runtime.
 *   <li>{@link StaticPathArchive} — an archive with a known, hardcoded path.
 * </ul>
 */
public abstract class Split {

  /** Returns the file system path to the split APK. */
  public abstract String getPath();

  /** Returns the path to an entry inside this split: {@code <path>!/<entryPath>}. */
  public String getEntryPath(String entryPath) {
    return getPath() + "!/" + entryPath;
  }

  /** Returns the full path to a library inside this split: {@code <path>!/lib/<abi>/<soName>}. */
  public String getLibraryPath(String abi, String soName) {
    return getEntryPath("lib/" + abi + "/" + soName);
  }

  /** Returns the library directory path inside this split: {@code <path>!/lib/<abi>/}. */
  public String getLibraryDirectory(String abi) {
    return getEntryPath("lib/" + abi + "/");
  }

  /** Finds the installed ABI split for the given feature. */
  public static Installed findAbiSplit(String feature) {
    return new Installed(Splits.findAbiSplit(feature));
  }

  /** Returns the installed base split for the given feature. */
  public static Installed findMasterSplit(String feature) {
    if ("base".equals(feature)) {
      return new Installed("base.apk");
    }
    return new Installed("split_" + feature + ".apk");
  }

  /** Creates a Split from a file path. Uses Installed if the file is an application split. */
  public static Split fromPath(File path) throws IOException {
    if (Splits.isApplicationSplit(path)) {
      return new Installed(path.getName());
    }
    return new StaticPathArchive(path);
  }

  /** An installed split whose path is resolved from application info at runtime. */
  public static class Installed extends Split {
    private final String mSplitName;

    public Installed(String splitName) {
      mSplitName = splitName;
    }

    @Override
    public String getPath() {
      return Splits.getSplitPath(mSplitName);
    }

    @Override
    public String toString() {
      return "installed split:" + mSplitName;
    }
  }

  /** An archive with a known, hardcoded path. */
  public static class StaticPathArchive extends Split {
    private final File mPath;

    public StaticPathArchive(File path) {
      mPath = path;
    }

    @Override
    public String getPath() {
      return mPath.getPath();
    }

    @Override
    public String toString() {
      return "static path archive:" + mPath;
    }
  }
}
