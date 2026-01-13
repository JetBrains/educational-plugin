package com.jetbrains.edu.learning.marketplace.metadata

import com.jetbrains.edu.learning.marketplace.settings.OpenOnSiteLinkSettings

class OpenOnSiteLinkMetadataProcessorTest : LinkMetadataProcessorTestBase() {
  override val linkParameterName: String = OpenOnSiteLinkMetadataProcessor.OPEN_ON_SITE_URL_PARAMETER_NAME

  override val link: String?
    get() = OpenOnSiteLinkSettings.getInstance(project).link
}