@file:Suppress("unused")

package com.jetbrains.edu.learning.stepik.api

import com.jetbrains.edu.learning.stepik.StepikUserInfo
import retrofit2.Call
import retrofit2.http.GET

interface StepikService {
  @GET("stepics/1/")
  fun getCurrentUser(): Call<UsersList>
}

class UsersList {
  lateinit var meta: Any
  lateinit var users: List<StepikUserInfo>
}
