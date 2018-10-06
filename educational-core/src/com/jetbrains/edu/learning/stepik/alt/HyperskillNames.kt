package com.jetbrains.edu.learning.stepik.alt

import org.jetbrains.ide.BuiltInServerManager

const val HYPERSKILL = "Hyperskill"
const val HYPERSKILL_URL = "https://hyperskill.org/"
private val port = BuiltInServerManager.getInstance().port
val REDIRECT_URI = "http://localhost:$port/api/edu/hyperskill/oauth"

var CLIENT_ID = "jcboczaSZYHmmCewusCNrE172yHkOONV7JY1ECh4"
val AUTHORISATION_CODE_URL = "https://hyperskill.org/oauth2/authorize/?" +
                             "client_id=$CLIENT_ID&redirect_uri=$REDIRECT_URI&grant_type=code&scope=read+write&response_type=code"

