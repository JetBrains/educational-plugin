package com.jetbrains.edu.learning.marketplace.api

import com.fasterxml.jackson.annotation.JsonProperty
import com.jetbrains.edu.learning.authUtils.OAuthAccount

const val ID = "id"
const val NAME = "name"
const val GUEST = "guest"
const val TYPE = "type"

class MarketplaceAccount : OAuthAccount<MarketplaceUserInfo>()

class MarketplaceUserInfo {
  @JsonProperty(ID)
  var id: String = ""

  @JsonProperty(NAME)
  var name: String = ""

  @JsonProperty(GUEST)
  var guest: Boolean = false

  @JsonProperty(TYPE)
  var type: String = ""

  override fun toString(): String {
    return name
  }
}