package com.jetbrains.edu.android

import com.android.tools.idea.sdk.IdeSdks
import java.io.File

// BACKCOMPAT: 2022.2. Inline it
fun setAndroidSdkPath(sdkLocation: File) {
  IdeSdks.getInstance().setAndroidSdkPath(sdkLocation)
}