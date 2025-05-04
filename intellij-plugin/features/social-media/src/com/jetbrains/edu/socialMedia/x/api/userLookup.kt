package com.jetbrains.edu.socialMedia.x.api

import com.fasterxml.jackson.annotation.JsonProperty

data class XUserLookup(
  @JsonProperty("data") val data: XUserData
)

data class XUserData(
  @JsonProperty("username") val username: String,
  @JsonProperty("name") val name: String
)
