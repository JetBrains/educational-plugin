package com.jetbrains.edu.scala.sbt

import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.project.Project
import org.jetbrains.sbt.project.SbtProjectSystem

val Project.isSbtProject: Boolean
  get() {
    val settings = ExternalSystemApiUtil.getSettings(this, SbtProjectSystem.Id()).getLinkedProjectsSettings()
    return settings.isNotEmpty()
  }