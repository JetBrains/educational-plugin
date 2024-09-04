package com.jetbrains.edu.learning.marketplace.lti

import com.fasterxml.jackson.annotation.JsonProperty

class LTISettings(
  @JsonProperty("launches")
  val launches: List<LTILaunch>
)