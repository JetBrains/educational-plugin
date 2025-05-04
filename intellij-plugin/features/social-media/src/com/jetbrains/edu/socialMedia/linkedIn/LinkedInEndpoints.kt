package com.jetbrains.edu.socialMedia.linkedIn

import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*
import retrofit2.http.Headers

interface LinkedInEndpoints {
  @GET("/v2/userinfo")
  fun getCurrentUserInfo(): Call<LinkedInUserInfo>

  @POST("/v2/assets?action=registerUpload")
  fun getImageUploadLink(@Body body: GetMediaUploadLink): Call<UploadResponse>

  @Headers("Content-Type:image/gif")
  @POST("{encodedPath}")
  fun uploadMedia(
    @Body image: RequestBody,
    @Path("encodedPath", encoded = true) encodedPath: String,
    @QueryMap params: Map<String, String>
  ): Call<Void>

  @POST("/v2/ugcPosts")
  @Headers("X-Restli-Protocol-Version:2.0.0")
  fun postTextWithMedia(@Body body: ShareMediaContentBody): Call<Any>
}