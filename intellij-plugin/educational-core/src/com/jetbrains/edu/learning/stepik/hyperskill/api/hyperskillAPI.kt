@file:Suppress("unused")

package com.jetbrains.edu.learning.stepik.hyperskill.api

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.application.impl.ApplicationInfoImpl
import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.AdditionalFilesUtils
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.authUtils.OAuthAccount
import com.jetbrains.edu.learning.courseFormat.EduFile
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.CHECK_PROFILE
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.TOPICS
import com.jetbrains.edu.learning.courseFormat.UserInfo
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillProject
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillStage
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillTopic
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.pluginVersion
import com.jetbrains.edu.learning.stepik.PyCharmStepOptions
import com.jetbrains.edu.learning.stepik.StepSource
import com.jetbrains.edu.learning.stepik.api.FILES
import com.jetbrains.edu.learning.stepik.api.REPLY
import com.jetbrains.edu.learning.stepik.api.STEPS
import org.jetbrains.annotations.TestOnly
import java.util.*

const val ACTION = "action"
const val CONTEXT = "context"
const val CLIENT = "client"
const val CLIENT_TIME = "client_time"
const val DESCRIPTION = "description"
const val DURATION = "duration"
const val EDUTOOLS = "edutools"
const val EDUTOOLS_VERSION = "edutools_version"
const val EMAIL = "email"
const val ENVIRONMENT = "environment"
const val FRAMEWORK = "framework"
const val FRONTEND_EVENTS = "frontend-events"
const val FULL_NAME = "fullname"
const val HAS_NEXT = "has_next"
const val ID = "id"
const val IDE_EDITION = "ide_edition"
const val IDE_FILES = "ide_files"
const val IDE_VERSION = "ide_version"
const val IS_COMPLETED = "is_completed"
const val IS_GUEST = "is_guest"
const val IS_NEXT = "is_next"
const val IS_RECOMMENDED = "is_recommended"
const val IS_REMOTE_TESTED = "is_remote_tested"
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
const val TIME_SPENT_EVENTS = "time-spent-events"
const val TITLE = "title"
const val TOKEN = "token"
const val TOPIC = "topic"
const val TOPIC_THEORY = "topic_theory"
const val UPDATED_AT = "updated_at"
const val URL = "url"
const val USERS = "users"
const val USE_IDE = "use_ide"

abstract class WithPaginationMetaData {
  @JsonProperty(META)
  lateinit var meta: PaginationMetaData
}

class PaginationMetaData {
  @JsonProperty(HAS_NEXT)
  var hasNext: Boolean = false
}

class HyperskillAccount : OAuthAccount<HyperskillUserInfo> {
  @TestOnly
  constructor() : super()

  constructor(tokenExpiresIn: Long) : super(tokenExpiresIn)

  override val servicePrefix: String = EduNames.JBA

}

class HyperskillUserInfo : UserInfo {
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

  override fun getFullName(): String {
    return fullname
  }

  override fun toString(): String {
    return getFullName()
  }
}

class HyperskillStepOptions : PyCharmStepOptions {
  constructor()

  constructor(project: Project, task: Task) : super(project, task) {
    val hyperskillAdditionalInfo = HyperskillAdditionalInfo()
    hyperskillAdditionalInfo.files = AdditionalFilesUtils.collectAdditionalFiles(task.course.configurator, project)
    hyperskill = hyperskillAdditionalInfo
  }
}

class HyperskillAdditionalInfo {
  @JsonProperty(FILES)
  var files: List<EduFile>? = null
}

class HyperskillStepSource : StepSource() {
  @JsonProperty(CHECK_PROFILE)
  var checkProfile: String = ""

  @JsonProperty(IS_NEXT)
  var isNext: Boolean = false

  @JsonProperty(IS_COMPLETED)
  var isCompleted: Boolean = false

  @JsonProperty(IS_RECOMMENDED)
  var isRecommended: Boolean = false

  @JsonProperty(IS_REMOTE_TESTED)
  var isRemoteTested: Boolean = false

  @JsonProperty(TITLE)
  var title: String = ""

  /**
   * Hyperskill field to indicate a task's framework.
   * Now it can be 'null' or 'Android'
   * The value 'Android' means that the task must be opened only in Android Studio as an Android project
   */
  @JsonProperty(FRAMEWORK)
  var framework: String? = null

  @JsonProperty(TOPIC)
  var topic: Int? = null

  @JsonProperty(TOPIC_THEORY)
  var topicTheory: Int? = null

  @JsonProperty(UPDATED_AT)
  override var updateDate: Date = Date(0)

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
  lateinit var profiles: List<HyperskillUserInfo>
}

class StagesList: WithPaginationMetaData() {
  @JsonProperty(STAGES)
  lateinit var stages: List<HyperskillStage>
}

class TopicsList: WithPaginationMetaData() {
  @JsonProperty(TOPICS)
  lateinit var topics: List<HyperskillTopic>
}

class ProjectsList {
  @JsonProperty(PROJECTS)
  lateinit var projects: List<HyperskillProject>
}

class HyperskillStepsList: WithPaginationMetaData() {
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

  override fun toString(): String {
    return "HyperskillFrontendEvent(action=$action, route='$route', clientTime=$clientTime, context=$context)"
  }

}

enum class HyperskillFrontendEventType {
  VIEW; // in the nearest future there will be more event types

  /**
   ** need this as enums are (de)serialized using toString
   *  see mapper settings in [com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector.objectMapper]
   */
  override fun toString(): String {
    return name.lowercase()
  }
}

class HyperskillTimeSpentEvent {
  @JsonProperty(STEP_ID)
  var step: Int = -1

  @JsonProperty(DURATION)
  var duration: Double = 0.0

  override fun toString(): String {
    return "HyperskillTimeSpentEvent(step=$step, duration=$duration)"
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

class HyperskillTimeSpentEventList {
  @JsonProperty(TIME_SPENT_EVENTS)
  lateinit var events: List<HyperskillTimeSpentEvent>
}
