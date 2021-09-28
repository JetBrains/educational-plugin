package com.jetbrains.edu.learning.marketplace.api

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface SubmissionsService {

  @POST("/api/document/create")
  fun createDocument(@Body submissionDocument: SubmissionDocument): Call<Document>

  @POST("/api/document/update")
  fun updateDocument(@Body submissionDocument: SubmissionDocument): Call<ResponseBody>

  @POST("/api/versions/list")
  fun getVersionsList(@Body document: Document): Call<Versions>

  @POST("/api/versions/get")
  fun getSubmissionContent(@Body submissionDocument: SubmissionDocument): Call<Content>

  @POST("/api/workspace/link")
  fun addPathToDocument(@Body descriptor: Descriptor): Call<ResponseBody>

  @POST("/api/workspace/list")
  fun getDescriptorsList(@Body path: DocumentPath): Call<Descriptors>
}