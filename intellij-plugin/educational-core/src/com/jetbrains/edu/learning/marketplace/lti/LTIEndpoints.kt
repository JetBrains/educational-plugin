package com.jetbrains.edu.learning.marketplace.lti

import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import retrofit2.http.Path

interface LTIEndpoints {

  @FormUrlEncoded
  @POST("/lti/grading/launchId/{launchId}/course/{courseEduId}/task/{taskEduId}")
  fun reportTaskSolved(
    @Path("launchId") launchId: String,
    @Path("courseEduId") courseEduId: Int,
    @Path("taskEduId") taskEduId: Int,
    @Field("completed") completed: Boolean
  ): Call<Unit>

}
