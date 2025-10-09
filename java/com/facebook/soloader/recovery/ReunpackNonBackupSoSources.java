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

package com.facebook.soloader.recovery;

import com.facebook.soloader.BackupSoSource;
import com.facebook.soloader.LogUtil;
import com.facebook.soloader.SoLoader;
import com.facebook.soloader.SoLoaderDSONotFoundError;
import com.facebook.soloader.SoLoaderULError;
import com.facebook.soloader.SoSource;
import com.facebook.soloader.UnpackingSoSource;

/**
 * RecoveryStrategy that detects cases when SoLoader failed to load a corrupted library, case in
 * which we try to re-unpack the libraries.
 */
public class ReunpackNonBackupSoSources implements RecoveryStrategy {

  @Override
  public boolean recover(UnsatisfiedLinkError error, SoSource[] soSources) {
    if (!(error instanceof SoLoaderULError)) {
      // Only recover from SoLoaderULE errors
      return false;
    }

    if (error instanceof SoLoaderDSONotFoundError) {
      // Do not attempt to recover if DSO is not found
      return false;
    }

    SoLoaderULError err = (SoLoaderULError) error;
    String soName = err.getSoName();

    LogUtil.e(
        SoLoader.TAG,
        "Reunpacking NonApk UnpackingSoSources due to "
            + error
            + ((soName == null) ? "" : (", retrying for specific library " + soName)));

    for (SoSource soSource : soSources) {
      if (soSource == null || !(soSource instanceof UnpackingSoSource)) {
        continue;
      }
      UnpackingSoSource uss = (UnpackingSoSource) soSource;
      if (uss instanceof BackupSoSource) {
        // Assume ReunpackBackupSoSources has already attempted to reunpack BackupSoSources
        continue;
      }
      try {
        LogUtil.e(SoLoader.TAG, "Runpacking " + uss.getName());
        uss.prepare(SoSource.PREPARE_FLAG_FORCE_REFRESH);
      } catch (Exception e) {
        // Catch a general error and log it, rather than failing during recovery and crashing the
        // app
        // in a different way.
        LogUtil.e(
            SoLoader.TAG,
            "Encountered an exception while reunpacking "
                + uss.getName()
                + " for library "
                + soName
                + ": ",
            e);
        return false;
      }
    }

    return true;
  }
}
