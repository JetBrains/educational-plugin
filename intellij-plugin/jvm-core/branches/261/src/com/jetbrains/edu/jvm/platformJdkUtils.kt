package com.jetbrains.edu.jvm

import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk

fun lookupJdkByPath(project: Project, homePath: String): Sdk =
  ExternalSystemJdkUtil.lookupJdkByPath(project, homePath)