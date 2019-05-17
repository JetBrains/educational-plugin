package com.jetbrains.edu.learning.stepik.hyperskill

import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.checker.TestsOutputParser
import org.jetbrains.ide.BuiltInServerManager

const val HYPERSKILL = "Hyperskill"
const val HYPERSKILL_TYPE = HYPERSKILL
const val HYPERSKILL_URL = "https://hyperskill.org/"
const val HYPERSKILL_PROJECTS_URL = "https://hyperskill.org/projects"
private val port = BuiltInServerManager.getInstance().port
val REDIRECT_URI = "http://localhost:$port/api/edu/hyperskill/oauth"

var CLIENT_ID = HyperskillOAuthBundle.valueOrDefault("clientId", "")
val AUTHORISATION_CODE_URL = "https://hyperskill.org/oauth2/authorize/?" +
                             "client_id=$CLIENT_ID&redirect_uri=$REDIRECT_URI&grant_type=code&scope=read+write&response_type=code"

const val HYPERSKILL_PROJECT_NOT_SUPPORTED = "Selected project is not supported yet. " +
                             "Please, <a href=\"$HYPERSKILL_PROJECTS_URL\">select another project</a> "

val HYPERSKILL_LANGUAGES = mapOf("java" to "${EduNames.JAVA} 11", "kotlin" to EduNames.KOTLIN, "python" to EduNames.PYTHON)

const val SUCCESS_MESSAGE = "<html>${TestsOutputParser.CONGRATULATIONS} " +
                            "Continue on <a href=\"https://hyperskill.org/learning-path\">Hyperskill</a>.</html>"