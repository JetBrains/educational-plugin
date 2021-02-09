package com.jetbrains.edu.scala.sbt

import com.intellij.openapi.externalSystem.model.ProjectSystemId
import org.jetbrains.sbt.project.SbtProjectSystem

val sbtProjectSystemId: ProjectSystemId get() = SbtProjectSystem.Id
