package com.jetbrains.edu.learning.stepik.hyperskill

import com.fasterxml.jackson.annotation.JsonProperty
import com.jetbrains.edu.learning.authUtils.OAuthAccount

const val USERS = "users"
const val STAGES = "stages"
const val TOPICS = "topics"
const val ID = "id"
const val TITLE = "title"
const val EMAIL = "email"
const val FULL_NAME = "fullname"
const val STAGE = "stage"
const val PROJECT = "project"
const val DESCRIPTION = "description"
const val LESSON_STEPIK_ID = "lesson_stepik_id"
const val USE_IDE = "use_ide"
const val THEORY_ID = "theory_id"

class HyperskillAccount : OAuthAccount<HyperskillUserInfo>()

class HyperskillUserInfo {
  @JsonProperty(ID)
  var id: Int = -1

  @JsonProperty(EMAIL)
  var email: String = ""

  @JsonProperty(FULL_NAME)
  var fullname: String = ""

  @JsonProperty(STAGE)
  var stage: HyperskillStage? = null

  @JsonProperty(PROJECT)
  var hyperskillProject: HyperskillProject? = null

  override fun toString(): String {
    return fullname
  }
}

class HyperskillStage {
  @JsonProperty(ID)
  var id: Int = -1

  @JsonProperty(TITLE)
  var title: String = ""

  @JsonProperty(PROJECT)
  var hyperskillProject: HyperskillProject? = null
}

class HyperskillProject {
  @JsonProperty(ID)
  var id: Int = -1

  @JsonProperty(TITLE)
  var title: String = ""

  @JsonProperty(DESCRIPTION)
  var description: String = ""

  @JsonProperty(LESSON_STEPIK_ID)
  var lesson: Int = -1

  @JsonProperty(USE_IDE)
  var useIde: Boolean = false
}

class UsersList {
  @JsonProperty(USERS)
  lateinit var users: List<HyperskillUserInfo>
}

class StagesList {
  @JsonProperty(STAGES)
  lateinit var stages: List<HyperskillStage>
}

class TopicsList {
  @JsonProperty(TOPICS)
  lateinit var topics: List<HyperskillTopic>
}

class HyperskillTopic {
  @JsonProperty(ID)
  var id: Int = -1

  @JsonProperty(TITLE)
  var title: String = ""

  @JsonProperty(THEORY_ID)
  var theoryId: Int? = null
}