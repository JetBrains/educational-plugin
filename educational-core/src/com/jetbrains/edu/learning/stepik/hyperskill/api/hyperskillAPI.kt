package com.jetbrains.edu.learning.stepik.hyperskill.api

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.application.impl.ApplicationInfoImpl
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.authUtils.OAuthAccount
import com.jetbrains.edu.learning.pluginVersion
import com.jetbrains.edu.learning.stepik.StepSource
import com.jetbrains.edu.learning.stepik.api.REPLY
import com.jetbrains.edu.learning.stepik.api.STEPS
import java.util.*

const val ACTION = "action"
const val CONTEXT = "context"
const val CLIENT = "client"
const val CLIENT_TIME = "client_time"
const val DESCRIPTION = "description"
const val EDUTOOLS = "edutools"
const val EDUTOOLS_VERSION = "edutools_version"
const val EMAIL = "email"
const val ENVIRONMENT = "environment"
const val FRONTEND_EVENTS = "frontend-events"
const val FULL_NAME = "fullname"
const val ID = "id"
const val IDE_EDITION = "ide_edition"
const val IDE_FILES = "ide_files"
const val IDE_VERSION = "ide_version"
const val IS_COMPLETED = "is_completed"
const val IS_GUEST = "is_guest"
const val IS_TEMPLATE_BASED = "is_template_based"
const val LANGUAGE = "language"
const val META = "meta"
const val PROFILES = "profiles"
const val PROJECT = "project"
const val PROJECTS = "projects"
const val ROUTE = "route"
const val SOLUTIONS = "solutions"
const val STAGES = "stages"
const val STEP_ID = "step"
const val THEORY_ID = "theory"
const val TITLE = "title"
const val TOKEN = "token"
const val TOPIC = "topic"
const val TOPICS = "topics"
const val TOPIC_THEORY = "topic_theory"
const val UPDATED_AT = "updated_at"
const val URL = "url"
const val USERS = "users"
const val USE_IDE = "use_ide"

class HyperskillAccount : OAuthAccount<HyperskillProfileInfo>()

class HyperskillProfileInfo {
  @JsonProperty(ID)
  var id: Int = -1

  @JsonProperty(EMAIL)
  var email: String = ""

  @JsonProperty(FULL_NAME)
  var fullname: String = ""

  @JsonProperty(IS_GUEST)
  var isGuest: Boolean = false

  @JsonProperty(PROJECT)
  var hyperskillProjectId: Int? = null

  override fun toString(): String {
    return fullname
  }
}

class HyperskillStage {

  @Suppress("unused") //used for deserialization
  constructor()

  constructor(stageId: Int, stageTitle: String, stageStepId: Int, isStageCompleted: Boolean = false) {
    id = stageId
    title = stageTitle
    stepId = stageStepId
    isCompleted = isStageCompleted
  }

  @JsonProperty(ID)
  var id: Int = -1

  @JsonProperty(TITLE)
  var title: String = ""

  @JsonProperty(STEP_ID)
  var stepId: Int = -1

  @JsonProperty(IS_COMPLETED)
  var isCompleted: Boolean = false
}

class HyperskillProject {
  @JsonProperty(ID)
  var id: Int = -1

  @JsonProperty(TITLE)
  var title: String = ""

  @JsonProperty(DESCRIPTION)
  var description: String = ""

  @JsonProperty(IDE_FILES)
  var ideFiles: String = ""

  // some projects might not support IDE
  @JsonProperty(USE_IDE)
  var useIde: Boolean = true

  @JsonProperty(LANGUAGE)
  var language: String = ""

  @JsonProperty(ENVIRONMENT)
  var environment: String? = null

  @JsonProperty(IS_TEMPLATE_BASED)
  var isTemplateBased: Boolean = false
}

class HyperskillTopic {
  @JsonProperty(ID)
  var id: Int = -1

  @JsonProperty(TITLE)
  var title: String = ""

  @JsonProperty(THEORY_ID)
  var theoryId: Int? = null
}

class HyperskillStepSource : StepSource() {
  @JsonProperty(TITLE)
  var title: String? = null

  @JsonProperty(TOPIC)
  var topic: Int? = null

  @JsonProperty(TOPIC_THEORY)
  var topicTheory: Int? = null

  @JsonProperty(UPDATED_AT)
  override var updateDate: Date? = null

}

class Solution {
  @JsonProperty(ID)
  var id: Int = 0

  @JsonProperty(REPLY)
  lateinit var reply: String
}

class User {
  @JsonProperty(ID)
  var id: Int = 0

  @JsonProperty(FULL_NAME)
  var fullname: String = ""
}

// lists

class ProfilesList {
  @JsonProperty(PROFILES)
  lateinit var profiles: List<HyperskillProfileInfo>
}

class StagesList {
  @JsonProperty(STAGES)
  lateinit var stages: List<HyperskillStage>
}

class TopicsList {
  @JsonProperty(META)
  lateinit var meta: Map<Any, Any>

  @JsonProperty(TOPICS)
  lateinit var topics: List<HyperskillTopic>
}

class ProjectsList {
  @JsonProperty(PROJECTS)
  lateinit var projects: List<HyperskillProject>
}

class HyperskillStepsList {
  @JsonProperty(STEPS)
  lateinit var steps: List<HyperskillStepSource>
}

class SolutionsList {
  @JsonProperty(SOLUTIONS)
  lateinit var solutions: List<Solution>
}

class UsersList {
  @JsonProperty(USERS)
  lateinit var users: List<User>
}

class WebSocketConfiguration {
  @JsonProperty(TOKEN)
  lateinit var token: String

  @JsonProperty(URL)
  lateinit var url: String
}

class HyperskillFrontendEvent {
  @JsonProperty(ACTION)
  lateinit var action: HyperskillFrontendEventType

  @JsonProperty(ROUTE)
  lateinit var route: String

  @JsonProperty(CLIENT_TIME)
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSX")
  var clientTime: Date = Date()

  @JsonProperty(CONTEXT)
  var context: HyperskillFrontendEventContext = HyperskillFrontendEventContext()
}

enum class HyperskillFrontendEventType {
  VIEW; // in the nearest future there will be more event types

  /**
   ** need this as enums are (de)serialized using toString
   *  see mapper settings in [com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector.objectMapper]
   */
  override fun toString(): String {
    return name.toLowerCase()
  }
}

class HyperskillFrontendEventContext {
  @JsonProperty(CLIENT)
  var client: String = EDUTOOLS

  @JsonProperty(IDE_VERSION)
  var ideVersion: String = with(ApplicationInfoImpl.getShadowInstance()) { "$versionName $fullVersion" }

  @JsonProperty(IDE_EDITION)
  var ideEdition: String = ApplicationNamesInfo.getInstance().editionName ?: ""

  @JsonProperty(EDUTOOLS_VERSION)
  var eduToolsVersion: String? = pluginVersion(EduNames.PLUGIN_ID)
}

class HyperskillFrontendEventList {
  @JsonProperty(FRONTEND_EVENTS)
  lateinit var events: List<HyperskillFrontendEvent>
}
