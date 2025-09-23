package com.jetbrains.edu.sql.jvm.gradle

import com.intellij.database.dataSource.DatabaseDriver
import com.intellij.database.dataSource.artifacts.DatabaseArtifactList
import com.intellij.database.dataSource.artifacts.DatabaseArtifactLoader
import com.intellij.database.dataSource.artifacts.DatabaseArtifactContext
import com.intellij.openapi.project.Project

fun DatabaseArtifactLoader.isValid(project: Project, version: DatabaseArtifactList.ArtifactVersion, driver: DatabaseDriver): Boolean {
  val context = DatabaseArtifactContext.getInstance(project, driver)
  return isValid(version, context)
}

fun DatabaseArtifactLoader.downloadArtifact(project: Project, artifact: DatabaseArtifactList.ArtifactVersion, driver: DatabaseDriver) {
  val context = DatabaseArtifactContext.getInstance(project, driver)
  downloadArtifact(artifact, context)
}