package com.jetbrains.edu.learning.authUtils


interface PasswordService {
  fun getSecret(userName: String, serviceNameForPasswordSafe: String): String?

  fun saveSecret(userName: String, serviceNameForPasswordSafe: String, secret: String)
}
