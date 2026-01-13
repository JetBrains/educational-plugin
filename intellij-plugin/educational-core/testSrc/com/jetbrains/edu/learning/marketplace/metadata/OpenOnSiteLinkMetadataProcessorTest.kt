package com.jetbrains.edu.learning.marketplace.metadata

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.marketplace.settings.OpenOnSiteLinkSettings

class OpenOnSiteLinkMetadataProcessorTest : LinkMetadataProcessorTestBase() {
  override val linkParameterName: String = "link"

  override fun getSavedLink(project: Project): String? {
    return OpenOnSiteLinkSettings.getInstance(project).link
  }
}