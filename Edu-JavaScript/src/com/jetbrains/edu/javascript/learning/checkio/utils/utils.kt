@file:JvmName("JsCheckiOUtils")

package com.jetbrains.edu.javascript.learning.checkio.utils

import com.jetbrains.edu.learning.checkio.account.CheckiOAccount
import com.jetbrains.edu.learning.checkio.utils.CheckiONames

val CheckiOAccount.profileUrl: String
  get() = "${JsCheckiONames.JS_CHECKIO_API_HOST}${CheckiONames.CHECKIO_USER}${userInfo.username}"

