package com.jetbrains.edu.learning.marketplace.metadata

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.marketplace.settings.LicenseLinkSettings

class LicenseLinkMetadataProcessorTest : LinkMetadataProcessorTestBase() {
  override val linkParameterName: String = "license_link"

  override fun getSavedLink(project: Project): String? {
    return LicenseLinkSettings.getInstance(project).link
  }
}