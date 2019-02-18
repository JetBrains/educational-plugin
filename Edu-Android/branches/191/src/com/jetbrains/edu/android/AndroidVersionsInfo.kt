package com.jetbrains.edu.android

import com.android.tools.idea.npw.FormFactor
import com.android.tools.idea.npw.platform.AndroidVersionsInfo

fun AndroidVersionsInfo.loadTargetVersions(callback: (List<AndroidVersionsInfo.VersionItem>) -> Unit) {
  loadLocalVersions()
  loadRemoteTargetVersions(FormFactor.MOBILE, FormFactor.MOBILE.minOfflineApiLevel, callback)
}
