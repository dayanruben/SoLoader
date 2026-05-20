# Copyright (c) Meta Platforms, Inc. and affiliates.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Ensure that methods from LollipopSysdeps don't get inlined. LollipopSysdeps.fallocate references
# an exception that isn't present prior to Lollipop, which trips up the verifier if the class is
# loaded on a pre-Lollipop OS.
-keep class com.facebook.soloader.SysUtil$LollipopSysdeps {
    public <methods>;
}

-keep class com.facebook.soloader.SoLoaderULError {
    *;
}

-keep class com.facebook.soloader.SoLoaderDSONotFoundError {
    *;
}

-keep class com.facebook.soloader.SoLoaderCorruptedLibNameError {
    *;
}

-keep class com.facebook.soloader.SoLoaderCorruptedLibFileError {
    *;
}

-keep class com.facebook.soloader.SoLoaderULErrorFactory {
    *;
}

-keep class com.facebook.soloader.MinElf$ElfError {
    *;
}
