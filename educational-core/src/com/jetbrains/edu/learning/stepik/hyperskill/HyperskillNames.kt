package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.ide.util.PropertiesComponent
import com.intellij.util.io.URLUtil
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.authUtils.CustomAuthorizationServer
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import org.jetbrains.ide.BuiltInServerManager

const val HYPERSKILL = "Hyperskill"
const val HYPERSKILL_TYPE = HYPERSKILL
const val HYPERSKILL_PROBLEMS = "Problems"
const val HYPERSKILL_URL_PROPERTY = "Hyperskill URL"
const val HYPERSKILL_DEFAULT_URL = "https://hyperskill.org/"
const val HYPERSKILL_PROJECTS_URL = "https://hyperskill.org/projects"
const val JBA_DEFAULT_URL = "https://www.jetbrains.com/academy/"
val HYPERSKILL_PROFILE_PATH = "${HYPERSKILL_URL}profile/"
private val port = BuiltInServerManager.getInstance().port
val REDIRECT_URI_DEFAULT = "http://localhost:$port/api/edu/hyperskill/oauth"
val HYPERSKILL_FAILOVER_PORTS = listOf(61904, 50605, 55795, 54687, 58511)

var CLIENT_ID = HyperskillOAuthBundle.valueOrDefault("hyperskillClientId", "")
var CLIENT_SECRET = HyperskillOAuthBundle.valueOrDefault("hyperskillClientSecret", "")
const val HYPERSKILL_PROJECT_NOT_SUPPORTED = "Selected project is not supported yet. " +
                                             "Please, <a href=\"$HYPERSKILL_PROJECTS_URL\">select another project</a> "
const val LOADING_PROJECT_STAGES = "Loading Project Stages"
const val SELECT_PROJECT = "Please <a href=\"$HYPERSKILL_PROJECTS_URL\">select a project</a> on ${EduNames.JBA}"
const val SYNCHRONIZE_JBA_ACCOUNT = "Synchronizing ${EduNames.JBA} Account"

val HYPERSKILL_LANGUAGES = mapOf("java" to "${EduNames.JAVA} 11", "kotlin" to EduNames.KOTLIN, "python" to EduNames.PYTHON,
                                 "javascript" to EduNames.JAVASCRIPT, "scala" to EduNames.SCALA,
                                 "FakeGradleBasedLanguage" to "FakeGradleBasedLanguage", "TEXT" to "TEXT", // last three needed for tests
                                 "Unsupported" to "Unsupported")

val HYPERSKILL_ENVIRONMENTS = mapOf("android" to EduNames.ANDROID, "unittest" to EduNames.UNITTEST)

val HYPERSKILL_URL: String
  get() = PropertiesComponent.getInstance().getValue(HYPERSKILL_URL_PROPERTY, HYPERSKILL_DEFAULT_URL)

val AUTHORISATION_CODE_URL: String
  get() = "${HYPERSKILL_URL}oauth2/authorize/?" +
          "client_id=$CLIENT_ID&redirect_uri=${URLUtil.encodeURIComponent(REDIRECT_URI)}&grant_type=code&scope=read+write&response_type=code"

val REDIRECT_URI: String
  get() = if (EduUtils.isAndroidStudio()) {
    getCustomServer().handlingUri
  }
  else {
    REDIRECT_URI_DEFAULT
  }

private fun createCustomServer(): CustomAuthorizationServer {
  return CustomAuthorizationServer.create(HYPERSKILL, "/api/edu/hyperskill/oauth")
  { code, _ -> if (HyperskillConnector.getInstance().login(code)) null else "Failed to login to ${EduNames.JBA}" }
}

private fun getCustomServer(): CustomAuthorizationServer {
  val startedServer = CustomAuthorizationServer.getServerIfStarted(HYPERSKILL)
  return startedServer ?: createCustomServer()
}

fun getCodeChallengesProjectName(language: String) = "${language.capitalize()} Code Challenges"
