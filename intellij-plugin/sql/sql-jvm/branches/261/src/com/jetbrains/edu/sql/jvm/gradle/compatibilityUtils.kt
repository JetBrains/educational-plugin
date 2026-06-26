package com.jetbrains.edu.sql.jvm.gradle

import com.intellij.database.dataSource.artifacts.DatabaseArtifactContext
import com.intellij.database.dataSource.artifacts.DatabaseArtifactList
import com.intellij.database.dataSource.artifacts.DatabaseArtifactLoader

fun DatabaseArtifactLoader.needToDownload(artifactVersion: DatabaseArtifactList.ArtifactVersion): Boolean {
  return !isValid(artifactVersion, DatabaseArtifactContext.getDefaultContext())
}
