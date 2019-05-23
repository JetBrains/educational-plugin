package com.jetbrains.edu.android

import com.android.ide.common.repository.GradleVersion
import com.android.repository.io.FileOpUtils
import com.android.tools.idea.projectsystem.GoogleMavenArtifactId
import com.android.tools.idea.templates.RepositoryUrlManager
import java.io.File

fun getLibraryRevision(
  artifactId: GoogleMavenArtifactId,
  sdkLocation: File,
  defaultVersion: String,
  versionFilter: ((GradleVersion) -> Boolean)? = null
): String {
  return RepositoryUrlManager.get().getLibraryRevision(
    artifactId.mavenGroupId,
    artifactId.mavenArtifactId,
    versionFilter,
    false,
    FileOpUtils.create()
  ) ?: defaultVersion
}
