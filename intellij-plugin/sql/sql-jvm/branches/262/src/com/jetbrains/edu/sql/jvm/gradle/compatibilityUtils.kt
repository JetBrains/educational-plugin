package com.jetbrains.edu.sql.jvm.gradle

import com.intellij.database.dataSource.artifacts.ArtifactFilesSource
import com.intellij.database.dataSource.artifacts.DatabaseArtifactContext
import com.intellij.database.dataSource.artifacts.DatabaseArtifactList
import com.intellij.database.dataSource.artifacts.DatabaseArtifactLoader

// BACKCOMPAT: 2026.1. Inline it
fun DatabaseArtifactLoader.needToDownload(artifactVersion: DatabaseArtifactList.ArtifactVersion): Boolean {
  return getFilesSource(artifactVersion, DatabaseArtifactContext.getDefaultContext()) == ArtifactFilesSource.REMOTE_STORAGE
}
