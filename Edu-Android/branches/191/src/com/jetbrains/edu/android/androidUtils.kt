package com.jetbrains.edu.android

import com.android.ide.common.repository.GradleCoordinate
import com.android.tools.idea.npw.FormFactor
import com.android.tools.idea.npw.model.NewProjectModel
import com.android.tools.idea.npw.platform.AndroidVersionsInfo
import com.android.tools.idea.templates.RepositoryUrlManager
import com.android.tools.idea.welcome.config.FirstRunWizardMode
import com.android.tools.idea.welcome.install.FirstRunWizardDefaults
import com.intellij.openapi.project.Project
import java.io.File

fun convertNameToPackage(name: String): String = NewProjectModel.toPackagePart(name)

fun AndroidVersionsInfo.loadRemoteVersions(
  formFactor: FormFactor,
  minSdkLevel: Int,
  callback: (List<AndroidVersionsInfo.VersionItem>) -> Unit
) = loadRemoteTargetVersions(formFactor, minSdkLevel, callback)

fun resolveDynamicCoordinateVersion(coordinate: GradleCoordinate, project: Project?): String? =
  RepositoryUrlManager.get().resolveDynamicCoordinateVersion(coordinate, project)

fun getInitialSdkLocation(mode: FirstRunWizardMode): File = FirstRunWizardDefaults.getInitialSdkLocation(mode)
