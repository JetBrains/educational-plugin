package com.jetbrains.edu.learning.marketplace.api

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.intellij.openapi.application.PermanentInstallationID
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.NlsSafe
import com.intellij.ui.JBAccountInfoService
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.jetbrains.edu.learning.authUtils.OAuthAccount
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.DEFAULT_ENVIRONMENT
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.marketplace.MARKETPLACE
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.learning.stepik.api.SOLUTION
import com.jetbrains.edu.learning.stepik.api.SUBMISSIONS
import com.jetbrains.edu.learning.submissions.SolutionFile
import com.jetbrains.edu.learning.submissions.Submission
import org.jetbrains.annotations.TestOnly
import java.util.*
import java.util.concurrent.TimeUnit

const val ID = "id"
const val NAME = "name"
private const val COMPATIBILITY = "compatibility"
private const val FORMAT_VERSION = "format_version"
private const val UPDATE_VERSION = "update_version"
private const val DATA = "data"
private const val ENVIRONMENT = "environment"
private const val GTE = "gte"
private const val IS_PRIVATE = "isPrivate"
private const val LANGUAGE = "language"
private const val PLUGIN_ID = "pluginId"
private const val PLUGINS = "plugins"
private const val PROGRAMMING_LANGUAGE = "programmingLanguage"
private const val PROGRAMMING_LANGUAGE_ID = "programmingLanguageId"
private const val PROGRAMMING_LANGUAGE_VERSION = "programmingLanguageVersion"
private const val QUERY = "query"
private const val SOLUTION_AWS_KEY = "solution_aws_key"
private const val TASK_ID = "task_id"
private const val TOTAL = "total"
private const val UPDATES = "updates"
private const val UUID = "uuid"
private const val VERSION = "version"
private const val HAS_NEXT = "has_next"

class MarketplaceAccount : OAuthAccount<JBAccountUserInfo> {
  @TestOnly
  constructor() : super()

  constructor(jbAccountUserInfo: JBAccountUserInfo) : super(jbAccountUserInfo)

  @NlsSafe
  override val servicePrefix: String = MARKETPLACE

  override fun getUserName(): String {
    return userInfo.getFullName()
  }

  fun isJBAccessTokenAvailable(jbAccountInfoService: JBAccountInfoService): Boolean {
    return getJBAccessToken(jbAccountInfoService) != null
  }

  @RequiresBackgroundThread
  fun getJBAccessToken(jbAccountInfoService: JBAccountInfoService): String? {
    var success = false
    return try {
      val jbAccessToken = jbAccountInfoService.accessToken.get(30, TimeUnit.SECONDS)
      success = jbAccessToken != null
      jbAccessToken
    }
    catch (e: Exception) {
      LOG.warn(e)
      null
    }
    finally {
      if (GetJBATokenSuccessRecorder.getInstance().updateState(success)) {
        EduCounterUsageCollector.obtainJBAToken(success)
      }
    }
  }

  fun checkTheSameUserAndUpdate(currentJbaUser: JBAccountUserInfo): Boolean {
    return if (userInfo.jbaLogin == currentJbaUser.jbaLogin) {
      //update username and email in case changed on remote
      userInfo.email = currentJbaUser.email
      userInfo.name = currentJbaUser.name
      true
    }
    else {
      false
    }
  }

  companion object {
    private val LOG = logger<MarketplaceAccount>()

    fun isJBALoggedIn(): Boolean = JBAccountInfoService.getInstance()?.userData != null

    fun getJBAIdToken(): String? = JBAccountInfoService.getInstance()?.idToken
  }
}

@Service
private class GetJBATokenSuccessRecorder {
  @Volatile
  private var currentState: Boolean? = null

  /**
   * Returns `true` if state changed, `false` otherwise
   */
  fun updateState(newState: Boolean): Boolean {
    val oldState = currentState
    currentState = newState
    return oldState != newState
  }

  companion object {
    fun getInstance(): GetJBATokenSuccessRecorder = service()
  }
}

class QueryData(graphqlQuery: String) {
  @JsonProperty(QUERY)
  var query: String = graphqlQuery
}

// downloaded CoursesInfos are not full-fledged EduCourses, they miss information, specific for the update,
// contained in the UpdateInfo (e.g. Course.formatVersion, stored in Compatibility.gte)
@JsonIgnoreProperties(ignoreUnknown = true)
class CoursesInfoList {
  @JsonProperty(TOTAL)
  var total: Int = -1

  @JsonProperty(PLUGINS)
  var courses: List<EduCourse> = emptyList()
}

@JsonIgnoreProperties(ignoreUnknown = true)
class CoursesData {
  @JsonProperty(DATA)
  lateinit var data: CoursesInfos
}

@JsonIgnoreProperties(ignoreUnknown = true)
class CoursesInfos {
  @JsonProperty(PLUGINS)
  lateinit var myCoursesInfoList: CoursesInfoList
}

class Author {
  @JsonProperty(NAME)
  var name: String = ""

  constructor()
  constructor(name: String) {
    this.name = name
  }
}

class Fields {
  @Suppress("unused")
  @Deprecated("Use languageId and languageVersion instead")
  @JsonProperty(PROGRAMMING_LANGUAGE)
  var programmingLanguage: String = ""

  @JsonProperty(PROGRAMMING_LANGUAGE_ID)
  var languageId: String? = null

  @JsonProperty(PROGRAMMING_LANGUAGE_VERSION)
  var languageVersion: String? = null

  @JsonProperty(LANGUAGE)
  var language: String = ""

  @JsonProperty(ENVIRONMENT)
  var environment: String? = DEFAULT_ENVIRONMENT

  @JsonProperty(IS_PRIVATE)
  var isPrivate: Boolean = false
}

class Organization {
  @JsonProperty(NAME)
  var name: String? = ""
}

@JsonIgnoreProperties(ignoreUnknown = true)
class UpdateData {
  @JsonProperty(DATA)
  lateinit var data: Updates
}

@JsonIgnoreProperties(ignoreUnknown = true)
class Updates {
  @JsonProperty(TOTAL)
  var total: Int = 0

  @JsonProperty(UPDATES)
  lateinit var updates: UpdatesList
}

@JsonIgnoreProperties(ignoreUnknown = true)
class UpdatesList {
  @JsonProperty(UPDATES)
  lateinit var updateInfoList: List<UpdateInfo>
}

@JsonIgnoreProperties(ignoreUnknown = true)
class UpdateInfo {
  @JsonProperty(ID)
  var updateId: Int = -1

  @JsonProperty(PLUGIN_ID)
  var pluginId: Int = -1

  @JsonProperty(VERSION)
  var version: Int = -1

  @JsonProperty(COMPATIBILITY)
  var compatibility: Compatibility = Compatibility()
}

@JsonIgnoreProperties(ignoreUnknown = true)
class Compatibility {
  @JsonProperty(GTE)
  var gte: Int = -1
}

@JsonIgnoreProperties(ignoreUnknown = true)
class CourseBean {
  @JsonProperty(ID)
  var id: Int = -1

  @JsonProperty(NAME)
  var name: String = ""
}

class MarketplaceSubmission : Submission {
  @JsonProperty(UPDATE_VERSION)
  var courseVersion: Int = 0

  @JsonProperty(TASK_ID)
  override var taskId: Int = -1

  // used in GET requests: solution files are being loaded from s3 on demand
  @JsonIgnore
  override var solutionFiles: List<SolutionFile>? = null

  // used in POST requests: we send solution files written as a string to submissions service and never download back
  @JsonProperty(SOLUTION)
  var solution: String = ""

  @JsonProperty(FORMAT_VERSION)
  override var formatVersion: Int = JSON_FORMAT_VERSION

  @JsonProperty(SOLUTION_AWS_KEY)
  var solutionKey: String = ""

  @JsonProperty(UUID)
  var uuid: String = ""

  constructor()

  // used to mark TheoryTasks solved
  constructor(task: TheoryTask) : this(task.id, CheckStatus.Solved, "", null, task.course.marketplaceCourseVersion)

  constructor(taskId: Int, checkStatus: CheckStatus, solutionText: String, solutionFiles: List<SolutionFile>?, courseVersion: Int) {
    this.taskId = taskId
    this.status = checkStatus.rawStatus
    this.solutionFiles = solutionFiles
    this.courseVersion = courseVersion
    solution = solutionText
    uuid = if (!isUnitTestMode) PermanentInstallationID.get() else "test-uuid"
  }
}

class MarketplaceSubmissionsList {
  @JsonProperty(HAS_NEXT)
  var hasNext: Boolean = false

  @JsonProperty(SUBMISSIONS)
  lateinit var submissions: List<MarketplaceSubmission>
}
