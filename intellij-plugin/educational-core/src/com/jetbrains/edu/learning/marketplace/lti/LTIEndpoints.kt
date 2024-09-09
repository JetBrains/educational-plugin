package com.jetbrains.edu.learning.marketplace.lti

import retrofit2.Call
import retrofit2.http.POST
import retrofit2.http.Path

interface LTIEndpoints {

  @POST("/lti/grading/launchId/{launchId}/course/{courseEduId}/task/{taskEduId}")
  fun reportTaskSolved(
    @Path("launchId") launchId: String,
    @Path("courseEduId") courseEduId: Int,
    @Path("taskEduId") taskEduId: Int
  ): Call<Unit>

}
