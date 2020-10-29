@file: JvmName("PyCheckiOUtils")

package com.jetbrains.edu.python.learning.checkio.utils

import com.jetbrains.edu.learning.checkio.account.CheckiOAccount
import com.jetbrains.edu.learning.checkio.utils.CheckiONames

val CheckiOAccount.profileUrl: String get() = "${PyCheckiONames.PY_CHECKIO_API_HOST}${CheckiONames.CHECKIO_USER}${userInfo.username}"