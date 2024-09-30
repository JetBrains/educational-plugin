package com.jetbrains.edu.learning.yaml.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import com.intellij.openapi.project.Project

interface YamlMigrationStep {
  fun getConfigName(): String
  fun migrationNeeded(project: Project, courseConfig: ObjectNode): Boolean
  fun transform(config: ObjectNode): ObjectNode
}