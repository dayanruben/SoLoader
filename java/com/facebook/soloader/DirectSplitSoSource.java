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

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.StrictMode;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.annotation.Nullable;

public class DirectSplitSoSource extends SoSource {
  protected final String mSplitName;

  protected @Nullable Manifest mManifest = null;
  protected @Nullable Map<String, Manifest.Library> mLibs = null;

  public DirectSplitSoSource(String splitName) {
    mSplitName = splitName;
  }

  Manifest getManifest() {
    if (mManifest == null) {
      throw new IllegalStateException("prepare not called");
    }
    return mManifest;
  }

  @Override
  protected void prepare(int flags) throws IOException {
    try (InputStream is =
        SoLoader.sApplicationContext.getAssets().open(mSplitName + ".soloader-manifest")) {
      mManifest = Manifest.read(is);
    }

    mLibs = new HashMap<String, Manifest.Library>();
    for (Manifest.Library lib : mManifest.libs) {
      mLibs.put(lib.name, lib);
    }
  }

  @Override
  public int loadLibrary(String soName, int loadFlags, StrictMode.ThreadPolicy threadPolicy)
      throws IOException {
    if (mLibs == null) {
      throw new IllegalStateException("prepare not called");
    }

    Manifest.Library library = mLibs.get(soName);
    if (library != null) {
      return loadLibraryImpl(library, loadFlags, threadPolicy);
    }

    return LOAD_RESULT_NOT_FOUND;
  }

  @SuppressLint("MissingSoLoaderLibrary")
  protected int loadLibraryImpl(
      Manifest.Library lib, int loadFlags, StrictMode.ThreadPolicy threadPolicy)
      throws IOException {
    if (lib.hasUnknownDeps()) {
      loadDependencies(lib, loadFlags, threadPolicy);
    }

    System.load(getLibraryPath(lib));
    return LOAD_RESULT_LOADED;
  }

  private void loadDependencies(
      Manifest.Library lib, int loadFlags, StrictMode.ThreadPolicy threadPolicy)
      throws IOException {
    try (ZipFile apk = new ZipFile(getSplitPath())) {
      try (ElfByteChannel bc = getElfByteChannel(apk, lib)) {
        NativeDeps.loadDependencies(lib.name, bc, loadFlags, threadPolicy);
      }
    }
  }

  @Override
  @Nullable
  public File unpackLibrary(String soName) {
    return getSoFileByName(soName);
  }

  @Override
  @Nullable
  protected File getSoFileByName(String soName) {
    String path = getLibraryPath(soName);
    return path == null ? null : new File(path);
  }

  @Override
  @Nullable
  public String getLibraryPath(String soName) {
    if (mLibs == null || mManifest == null) {
      throw new IllegalStateException("prepare not called");
    }

    Manifest.Library library = mLibs.get(soName);
    if (library != null) {
      return getLibraryPath(library);
    }

    return null;
  }

  private String getLibraryPath(Manifest.Library lib) {
    if (mManifest == null) {
      throw new IllegalStateException("prepare not called");
    }
    return getSplitPath() + "!/lib/" + mManifest.arch + "/" + lib.name;
  }

  private String getSplitPath() {
    return getSplitPath(mSplitName);
  }

  static String getSplitPath(String splitName) {
    if ("base".equals(splitName)) {
      if (SoLoader.sApplicationInfoProvider == null) {
        throw new IllegalStateException("SoLoader not initialized");
      }
      return SoLoader.sApplicationInfoProvider.get().sourceDir;
    }

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
      throw new IllegalStateException("Splits are not supported before Android L");
    }

    String[] splits = SoLoader.sApplicationContext.getApplicationInfo().splitSourceDirs;
    if (splits == null) {
      throw new IllegalStateException("No splits avaiable");
    }

    String fileName = "split_" + splitName + ".apk";
    for (String split : splits) {
      if (split.endsWith(fileName)) {
        return split;
      }
    }

    throw new IllegalStateException("Could not find " + splitName + " split");
  }

  @Override
  @Nullable
  public String[] getLibraryDependencies(String soName) throws IOException {
    if (mLibs == null) {
      throw new IllegalStateException("prepare not called");
    }

    Manifest.Library library = mLibs.get(soName);
    if (library != null) {
      return getLibraryDependencies(library);
    }

    return null;
  }

  protected String[] getLibraryDependencies(Manifest.Library lib) throws IOException {
    try (ZipFile apk = new ZipFile(getSplitPath())) {
      try (ElfByteChannel bc = getElfByteChannel(apk, lib)) {
        return NativeDeps.getDependencies(lib.name, bc);
      }
    }
  }

  private ElfByteChannel getElfByteChannel(ZipFile apk, Manifest.Library lib) throws IOException {
    if (mManifest == null) {
      throw new IllegalStateException("prepare not called");
    }
    final ZipEntry entry = apk.getEntry("lib/" + mManifest.arch + "/" + lib.name);
    return new ElfZipFileChannel(apk, entry);
  }

  @Override
  public String[] getSoSourceAbis() {
    if (mManifest == null) {
      throw new IllegalStateException("prepare not called");
    }
    return new String[] {mManifest.arch};
  }

  @Override
  public String getName() {
    return "DirectSplitSoSource";
  }
}
