package com.jetbrains.edu.jvm

import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project

fun ensureToolWindowInitialized(project: Project, externalSystemId: ProjectSystemId) {
  ExternalSystemUtil.ensureToolWindowInitialized(project, externalSystemId)
}
