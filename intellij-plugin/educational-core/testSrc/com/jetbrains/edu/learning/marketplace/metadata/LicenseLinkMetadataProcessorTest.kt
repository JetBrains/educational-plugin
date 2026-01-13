package com.jetbrains.edu.learning.marketplace.metadata

import com.jetbrains.edu.learning.marketplace.settings.LicenseLinkSettings

class LicenseLinkMetadataProcessorTest : LinkMetadataProcessorTestBase() {
  override val linkParameterName: String = LicenseLinkMetadataProcessor.LICENSE_URL_PARAMETER_NAME

  override val link: String?
    get() = LicenseLinkSettings.getInstance(project).link
}