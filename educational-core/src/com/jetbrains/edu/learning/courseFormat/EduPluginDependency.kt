package com.jetbrains.edu.learning.courseFormat

import com.intellij.externalDependencies.DependencyOnPlugin

open class EduPluginDependency {
  open var id: String = ""
  open var minVersion: String? = null
  open var maxVersion: String? = null

  //needed for deserialization
  constructor()

  @Suppress("LeakingThis")
  constructor(dependency: DependencyOnPlugin) {
    id = dependency.pluginId
    minVersion = dependency.minVersion
    maxVersion = dependency.maxVersion
  }
}