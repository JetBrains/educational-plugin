package com.jetbrains.edu.socialMedia.x.api

import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*

interface XV2 {
  /**
   * Returns information about the requesting User
   *
   * [User lookup me](https://docs.x.com/x-api/users/user-lookup-me)
   */
  @GET("/2/users/me")
  fun usersMe(): Call<XUserLookup>

  /**
   * Initialize a media upload [request](https://docs.x.com/x-api/media/media-upload-initialize)
   */
  @POST("/2/media/upload/initialize")
  fun mediaUploadInitialize(@Body request: XMediaUploadInitializeRequest): Call<XMediaUploadResponse>

  /**
   * Loading media chunk [request](https://docs.x.com/x-api/media/media-upload-append)
   */
  @Multipart
  @POST("/2/media/upload/{id}/append")
  fun mediaUploadAppend(
    @Path("id") mediaId: String,
    @Part("segment_index") segmentIndex: Int,
    @Part("media") media: RequestBody
  ): Call<XMediaUploadAppendResponse>

  /**
   * Finalize a media upload [request](https://docs.x.com/x-api/media/media-upload-finalize)
   */
  @POST("/2/media/upload/{id}/finalize")
  fun mediaUploadFinalize(@Path("id") mediaId: String): Call<XMediaUploadResponse>

  /**
   * [Media Upload Status](https://docs.x.com/x-api/media/media-upload-status)
   */
  @GET("/2/media/upload")
  fun mediaUploadStatus(@Query("media_id") mediaId: String): Call<XMediaUploadResponse>

  /**
   * Causes the User to create a Post under the authorized account
   *
   * [Creation of a Post](https://docs.x.com/x-api/posts/creation-of-a-post)
   */
  @POST("/2/tweets")
  fun postTweet(@Body tweet: Tweet): Call<TweetResponse>
}
