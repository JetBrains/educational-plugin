package com.jetbrains.edu.learning.marketplace.license

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.marketplace.settings.LicenseLinkSettings

fun requiresLicense(project: Project): Boolean {
  return LicenseLinkSettings.getInstance(project).link != null
}