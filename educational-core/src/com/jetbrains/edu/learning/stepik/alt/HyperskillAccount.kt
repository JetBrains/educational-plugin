package com.jetbrains.edu.learning.stepik.alt

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.jetbrains.edu.learning.OauthAccount

class HyperskillAccount : OauthAccount<HyperskillUserInfo>()

@JsonIgnoreProperties(ignoreUnknown = true)
class HyperskillUserInfo {
  var id: Int = -1
  var email: String = ""
  var fullname: String = ""
  var stage: HyperskillStage? = null
  var project: HyperskillProject? = null

  override fun toString(): String {
    return fullname
  }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class HyperskillStage(var id: Int = -1, var title: String = "")

@JsonIgnoreProperties(ignoreUnknown = true)
data class HyperskillProject(var id: Int = -1, var title: String = "", var description: String = "",
                             var lesson: Int = -1)