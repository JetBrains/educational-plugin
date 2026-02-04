package com.jetbrains.edu.learning.courseFormat

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.EMAIL
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.NAME
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.URL


data class Vendor(
  @JsonProperty(NAME)
  val name: String = "",
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonProperty(EMAIL)
  val email: String? = null,
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonProperty(URL)
  val url: String? = null
) {
  override fun toString(): String = name
}
