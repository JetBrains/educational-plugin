package com.jetbrains.edu.learning.marketplace.lti

import com.fasterxml.jackson.annotation.JsonProperty

data class LTILaunch(
  @JsonProperty("id")
  val id: String,
  @JsonProperty("lms_description")
  val lmsDescription: String
)
