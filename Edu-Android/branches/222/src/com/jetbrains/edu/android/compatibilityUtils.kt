package com.jetbrains.edu.android

import com.android.tools.idea.sdk.IdeSdks
import java.io.File

fun setAndroidSdkPath(sdkLocation: File) {
  IdeSdks.getInstance().setAndroidSdkPath(sdkLocation, null)
}