package com.jetbrains.edu.learning.authUtils


interface AccountService {
  fun getSecret(userName: String?, serviceNameForPasswordSafe: String?): String?
}
