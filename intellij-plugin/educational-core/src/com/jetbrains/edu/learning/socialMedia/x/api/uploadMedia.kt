package com.jetbrains.edu.learning.socialMedia.x.api

import com.fasterxml.jackson.annotation.JsonProperty

data class XMediaUploadResponse(
  @JsonProperty("data") val data: XUploadData
)

data class XUploadData(
  @JsonProperty("id") val id: String,
  @JsonProperty("media_key") val mediaKey: String,
  @JsonProperty("expires_after_secs") val expiresAfterSecs: Long,
  @JsonProperty("processing_info") val processingInfo: XProcessingInfo?
)

data class XProcessingInfo(
  @JsonProperty("state") val state: PendingState,
  @JsonProperty("check_after_secs") val checkAfterSecs: Long,
)

enum class PendingState {
  @JsonProperty("pending") PENDING,
  @JsonProperty("in_progress") IN_PROGRESS,
  @JsonProperty("succeeded") SUCCEEDED,
  @JsonProperty("failed") FAILED;
}

enum class UploadCommand {
  INIT,
  APPEND,
  FINALIZE
}