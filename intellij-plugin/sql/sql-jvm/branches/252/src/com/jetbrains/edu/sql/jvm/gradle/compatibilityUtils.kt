package com.jetbrains.edu.sql.jvm.gradle

import com.intellij.database.dataSource.artifacts.DatabaseArtifactList
import com.intellij.database.dataSource.artifacts.DatabaseArtifactLoader

fun isValid(loader: DatabaseArtifactLoader, version: DatabaseArtifactList.ArtifactVersion): Boolean = loader.isValid(version)

fun downloadArtifact(loader: DatabaseArtifactLoader, artifact: DatabaseArtifactList.ArtifactVersion) {
  loader.downloadArtifact(artifact)
}