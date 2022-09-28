package com.jetbrains.edu.learning.marketplace.api

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.intellij.credentialStore.Credentials
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.util.NlsSafe
import com.jetbrains.edu.learning.authUtils.OAuthAccount
import com.jetbrains.edu.learning.authUtils.TokenInfo
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.DEFAULT_ENVIRONMENT
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.marketplace.MARKETPLACE
import com.jetbrains.edu.learning.stepik.api.SOLUTION
import com.jetbrains.edu.learning.submissions.SolutionFile
import com.jetbrains.edu.learning.submissions.Submission
import org.jetbrains.annotations.TestOnly
import java.util.*

const val ID = "id"
const val NAME = "name"
private const val CONTENT = "content"
private const val COMPATIBILITY = "compatibility"
private const val COURSE_VERSION = "course_version"
private const val DATA = "data"
private const val DESCRIPTORS = "descriptors"
private const val ENVIRONMENT = "environment"
private const val GTE = "gte"
private const val GUEST = "guest"
private const val IS_PRIVATE = "isPrivate"
private const val LANGUAGE = "language"
private const val PATH = "path"
private const val PLUGIN_ID = "pluginId"
private const val PLUGINS = "plugins"
private const val PROGRAMMING_LANGUAGE = "programmingLanguage"
private const val QUERY = "query"
private const val TASK_ID = "task_id"
private const val TOTAL = "total"
private const val TIMESTAMP = "timestamp"
private const val TYPE = "type"
private const val UPDATES = "updates"
private const val VERSION = "version"
private const val VERSIONS = "versions"

class MarketplaceAccount : OAuthAccount<MarketplaceUserInfo> {
  @TestOnly
  constructor() : super()

  constructor(tokenExpiresIn: Long) : super(tokenExpiresIn)

  private val serviceNameForJwtToken @NlsSafe get() = "$servicePrefix jwt token"
  private val serviceNameForHubIdToken @NlsSafe get() = "$servicePrefix hub id token"
  private val serviceNameForJBAccountToken @NlsSafe get() = "$servicePrefix jb account id token"

  @NlsSafe
  override val servicePrefix: String = MARKETPLACE

  override fun getUserName(): String {
    return userInfo.getFullName()
  }

  override fun saveTokens(tokenInfo: TokenInfo) {
    super.saveTokens(tokenInfo)

    val userName = getUserName()
    PasswordSafe.instance.set(credentialAttributes(userName, serviceNameForHubIdToken), Credentials(userName, tokenInfo.idToken))
  }

  fun getHubIdToken(): String? {
    return getSecret(getUserName(), serviceNameForHubIdToken)
  }

  fun getJBAccountToken(): String? {
    return getSecret(getUserName(), serviceNameForJBAccountToken)
  }

  fun saveJBAccountToken(jBAccountToken: String) {
    val userName = getUserName()
    PasswordSafe.instance.set(credentialAttributes(userName, serviceNameForJBAccountToken), Credentials(userName, jBAccountToken))
  }

  fun saveJwtToken(jwtToken: String) {
    val userName = getUserName()
    PasswordSafe.instance.set(credentialAttributes(userName, serviceNameForJwtToken), Credentials(userName, jwtToken))
  }

  fun getJwtToken(): String? {
    return getSecret(getUserName(), serviceNameForJwtToken)
  }

  fun isJwtTokenProvided(): Boolean {
    return !getJwtToken().isNullOrEmpty()
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
  @JsonProperty(PROGRAMMING_LANGUAGE)
  var programmingLanguage: String = ""

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

class Document() {
  @JsonProperty(ID)
  var id: String = ""

  constructor(documentId: String): this() {
    id = documentId
  }
}

@JsonIgnoreProperties(ignoreUnknown = true)
class Descriptors {
  @JsonProperty(DESCRIPTORS)
  var descriptorsList: List<Descriptor> = emptyList()
}

@JsonIgnoreProperties(ignoreUnknown = true)
class Descriptor() {
  @JsonProperty(ID)
  var id: String = ""

  @JsonProperty(PATH)
  var path: String = ""

  constructor(documentId: String, documentPath: String): this() {
    id = documentId
    path = documentPath
  }
}

class DocumentPath(documentPath: String) {
  @JsonProperty(PATH)
  var path: String = documentPath
}

class SubmissionDocument() {
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonProperty(ID)
  var id: String? = null

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonProperty(VERSION)
  var version: String? = null

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonProperty(CONTENT)
  var content: String? = null

  constructor(docId: String?, versionId: String? = null, submissionContent: String? = null) : this() {
    id = docId
    version = versionId
    content = submissionContent
  }
}

class MarketplaceSubmission : Submission {
  @JsonProperty(COURSE_VERSION)
  var courseVersion: Int = 0

  @JsonProperty(TASK_ID)
  override var taskId: Int = -1

  @JsonProperty(SOLUTION)
  override var solutionFiles: List<SolutionFile>? = null

  @JsonProperty(VERSION)
  override var formatVersion: Int = JSON_FORMAT_VERSION

  constructor()

  // used to mark TheoryTasks solved
  constructor(task: TheoryTask) : this(task.id, CheckStatus.Solved, null, task.course.marketplaceCourseVersion)

  constructor(taskId: Int, checkStatus: CheckStatus, files: List<SolutionFile>?, courseVersion: Int) {
    time = Date()
    id = this.hashCode()
    solutionFiles = files?.filter { it.isVisible }
    this.taskId = taskId
    this.status = checkStatus.rawStatus
    this.courseVersion = courseVersion
  }
}

@JsonIgnoreProperties(ignoreUnknown = true)
class Content {
  @JsonProperty(CONTENT)
  lateinit var content: String
}

@JsonIgnoreProperties(ignoreUnknown = true)
class Versions {
  @JsonProperty(VERSIONS)
  var versionsList: List<Version> = emptyList()
}

class Version() {
  @JsonProperty(ID)
  var id: String = ""

  @JsonProperty(TIMESTAMP)
  var timestamp: Long = -1

  constructor(versionId: String, versionTimestamp: Long): this() {
    id = versionId
    timestamp = versionTimestamp
  }
}
