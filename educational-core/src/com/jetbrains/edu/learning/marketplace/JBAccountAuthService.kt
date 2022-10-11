package com.jetbrains.edu.learning.marketplace

import com.intellij.openapi.components.service
import com.intellij.ui.JBAccountInfoService

class JBAccountAuthService {

  companion object {
    fun getInstance(): JBAccountAuthService = service()
  }

  fun isLoggedIn() : Boolean {
    return JBAccountInfoService.getInstance()?.userData != null
  }

  fun getUserData(): JBAccountInfoService.JBAData? {
    return JBAccountInfoService.getInstance()?.userData
  }

  fun getAccessToken(): String? {
    return try {
      JBAccountInfoService.getInstance()?.accessToken?.get()
    }
    catch (e: Exception) {
      null
    }
  }

  fun login() {
    if (!isLoggedIn()) {
      JBAccountInfoService.getInstance()?.invokeJBALogin({}, {})
    }
  }

  fun isLoginAvailable(): Boolean = JBAccountInfoService.getInstance() != null
}