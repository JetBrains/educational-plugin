package com.jetbrains.edu.sql.jvm.gradle

import com.intellij.database.dataSource.artifacts.DatabaseArtifactList
import com.intellij.database.dataSource.artifacts.DatabaseArtifactLoader
import com.intellij.database.dataSource.artifacts.DatabaseArtifactContext

// BACKCOMPAT: 2025.2. Inline it
fun isValid(loader: DatabaseArtifactLoader, version: DatabaseArtifactList.ArtifactVersion): Boolean {
  return loader.isValid(version, DatabaseArtifactContext.getDefaultContext())
}

// BACKCOMPAT: 2025.2. Inline it
fun downloadArtifact(loader: DatabaseArtifactLoader, artifact: DatabaseArtifactList.ArtifactVersion) {
  loader.downloadArtifact(artifact, DatabaseArtifactContext.getDefaultContext())
}