package com.jetbrains.edu.android

import com.android.ide.common.repository.GradleCoordinate
import com.android.tools.idea.npw.FormFactor
import com.android.tools.idea.npw.model.NewProjectModel
import com.android.tools.idea.npw.platform.AndroidVersionsInfo
import com.android.tools.idea.sdk.AndroidSdks
import com.android.tools.idea.templates.RepositoryUrlManager
import com.android.tools.idea.welcome.config.FirstRunWizardMode
import com.intellij.openapi.project.Project
import java.io.File
import java.util.function.Consumer

fun convertNameToPackage(name: String): String = NewProjectModel.nameToJavaPackage(name)

fun AndroidVersionsInfo.loadRemoteVersions(
  formFactor: FormFactor,
  minSdkLevel: Int,
  callback: (List<AndroidVersionsInfo.VersionItem>) -> Unit
) = loadRemoteTargetVersions(formFactor, minSdkLevel, Consumer { callback(it) })

fun resolveDynamicCoordinateVersion(coordinate: GradleCoordinate, project: Project?): String? =
  RepositoryUrlManager.get().resolveDynamicCoordinateVersion(coordinate, project, AndroidSdks.getInstance().tryToChooseSdkHandler())

fun getInitialSdkLocation(mode: FirstRunWizardMode): File = getInitialSdkLocation(mode)
