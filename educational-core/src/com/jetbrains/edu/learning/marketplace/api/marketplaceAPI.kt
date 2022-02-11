package com.jetbrains.edu.learning.marketplace.api

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.intellij.credentialStore.Credentials
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.util.NlsSafe
import com.jetbrains.edu.learning.EduNames.DEFAULT_ENVIRONMENT
import com.jetbrains.edu.learning.JSON_FORMAT_VERSION
import com.jetbrains.edu.learning.UserInfo
import com.jetbrains.edu.learning.authUtils.OAuthAccount
import com.jetbrains.edu.learning.authUtils.TokenInfo
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.marketplace.MARKETPLACE
import com.jetbrains.edu.learning.stepik.api.SOLUTION
import com.jetbrains.edu.learning.submissions.SolutionFile
import com.jetbrains.edu.learning.submissions.Submission
import org.jetbrains.annotations.TestOnly
import java.util.*

const val AUTHORS = "authors"
const val CONTENT = "content"
const val CREATE_DATE = "cdate"
const val COURSE_VERSION = "course_version"
const val DATA = "data"
const val DESCRIPTION = "description"
const val DESCRIPTORS = "descriptors"
const val DOWNLOADS = "downloads"
const val ENVIRONMENT = "environment"
const val FIELDS = "fields"
const val GUEST = "guest"
const val ID = "id"
const val IS_PRIVATE = "isPrivate"
const val LANGUAGE = "language"
const val LAST_UPDATE_DATE = "lastUpdateDate"
const val LICENSE = "license"
const val LINK = "link"
const val MARKETPLACE_COURSE_VERSION = "course_version"
const val NAME = "name"
const val ORGANIZATION = "organization"
const val PATH = "path"
const val PLUGINS = "plugins"
const val PROGRAMMING_LANGUAGE = "programmingLanguage"
const val QUERY = "query"
const val RATING = "rating"
const val TASK_ID = "task_id"
const val TOTAL = "total"
const val TIMESTAMP = "timestamp"
const val TYPE = "type"
const val UPDATES = "updates"
const val VERSION = "version"
const val VERSIONS = "versions"

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

class QueryData(graphqlQuery: String) {
  @JsonProperty(QUERY)
  var query: String = graphqlQuery
}

@JsonIgnoreProperties(ignoreUnknown = true)
class CoursesList {
  @JsonProperty(TOTAL)
  var total: Int = -1

  @JsonProperty(PLUGINS)
  var courses: List<EduCourse> = emptyList()
}

@JsonIgnoreProperties(ignoreUnknown = true)
class CoursesData {
  @JsonProperty(DATA)
  lateinit var data: Courses
}

@JsonIgnoreProperties(ignoreUnknown = true)
class Courses {
  @JsonProperty(PLUGINS)
  lateinit var coursesList: CoursesList
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

  @JsonProperty(VERSION)
  var version: Int = -1
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

  constructor(taskId: Int, status: CheckStatus, files: List<SolutionFile>?, courseVersion: Int) {
    time = Date()
    id = this.hashCode()
    solutionFiles = files?.filter { it.isVisible }
    this.taskId = taskId
    this.status = status.toString()
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
