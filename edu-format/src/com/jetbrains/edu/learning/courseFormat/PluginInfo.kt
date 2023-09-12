package com.jetbrains.edu.learning.courseFormat

open class PluginInfo {
  open var stringId: String = ""
  open var displayName: String? = null
  open var minVersion: String? = null
  open var maxVersion: String? = null

  constructor()

  @Suppress("LeakingThis")
  constructor(stringId: String, displayName: String? = null, minVersion: String? = null, maxVersion: String? = null) {
    this.stringId = stringId
    this.displayName = displayName
    this.minVersion = minVersion
    this.maxVersion = maxVersion
  }
}
