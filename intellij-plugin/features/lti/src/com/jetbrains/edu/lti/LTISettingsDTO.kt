package com.jetbrains.edu.lti

data class LTISettingsDTO(
  val launchId: String,
  val lmsDescription: String?,
  val onlineService: LTIOnlineService,
  val returnLink: String?
)