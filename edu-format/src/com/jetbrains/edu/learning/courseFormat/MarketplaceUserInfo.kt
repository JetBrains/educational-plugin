package com.jetbrains.edu.learning.courseFormat

import com.fasterxml.jackson.annotation.JsonProperty
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.ID
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.NAME
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.GUEST
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.TYPE

class MarketplaceUserInfo() : UserInfo {
  @JsonProperty(ID)
  var id: String = ""

  @JsonProperty(NAME)
  var name: String = ""

  @JsonProperty(GUEST)
  override var isGuest: Boolean = false

  @JsonProperty(TYPE)
  var type: String = ""

  constructor(userName: String) : this() {
    name = userName
  }

  override fun getFullName(): String = name

  override fun toString(): String {
    return name
  }
}