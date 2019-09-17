package com.jetbrains.edu.android

import com.android.tools.idea.npw.FormFactor
import com.android.tools.idea.npw.model.NewProjectModel
import com.android.tools.idea.npw.platform.AndroidVersionsInfo
import java.util.function.Consumer

fun convertNameToPackage(name: String): String = NewProjectModel.nameToJavaPackage(name)

fun AndroidVersionsInfo.loadRemoteVersions(
  formFactor: FormFactor,
  minSdkLevel: Int,
  callback: (List<AndroidVersionsInfo.VersionItem>) -> Unit
) = loadRemoteTargetVersions(formFactor, minSdkLevel, Consumer { callback(it) })
