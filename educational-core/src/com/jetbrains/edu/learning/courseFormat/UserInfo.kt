package com.jetbrains.edu.learning.courseFormat

interface UserInfo {
  var isGuest: Boolean

  fun getFullName(): String
}
