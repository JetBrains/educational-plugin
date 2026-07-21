package com.jetbrains.edu.learning.marketplace.api

import com.intellij.ui.JBAccountInfoService
import java.util.concurrent.Future


internal fun JBAccountInfoService.getJBAuthAccessToken(): Future<String?> = accessToken
