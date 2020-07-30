package com.jetbrains.edu.scala.sbt

import com.intellij.openapi.project.Project
import org.jetbrains.sbt.SbtUtil

val Project.isSbtProject: Boolean
  get() {
    return SbtUtil.isSbtProject(this)
  }