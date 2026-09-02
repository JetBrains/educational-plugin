package com.jetbrains.edu.learning.marketplace.api

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

private const val TYPE = "type"
private const val CERTIFICATE_ID = "certificateId"
private const val IS_FIRST_CLIENT_REQUEST = "isFirstClientRequest"
private const val REQUIRED_PERCENT = "requiredPercent"

private const val READY = "ready"
private const val PENDING = "pending"
private const val NOT_READY = "not_ready"
private const val SUPPORTED = "supported"
private const val UNSUPPORTED = "unsupported"

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = TYPE, defaultImpl = CourseCertificateIssueResponse.Unsupported::class)
@JsonSubTypes(
  JsonSubTypes.Type(value = CourseCertificateIssueResponse.CertificateReady::class, name = READY),
  JsonSubTypes.Type(value = CourseCertificateIssueResponse.CertificatePending::class, name = PENDING),
  JsonSubTypes.Type(value = CourseCertificateIssueResponse.NotReady::class, name = NOT_READY),
  JsonSubTypes.Type(value = CourseCertificateIssueResponse.Unsupported::class, name = UNSUPPORTED),
)
sealed interface CourseCertificateIssueResponse {

  /**
   * The certificate is issued or is being generated
   */
  sealed interface Issued : CourseCertificateIssueResponse {
    val certificateId: String
    val isFirstClientRequest: Boolean
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  data class CertificateReady(
    @JsonProperty(CERTIFICATE_ID) override val certificateId: String,
    @JsonProperty(IS_FIRST_CLIENT_REQUEST) override val isFirstClientRequest: Boolean
  ) : Issued

  @JsonIgnoreProperties(ignoreUnknown = true)
  data class CertificatePending(
    @JsonProperty(CERTIFICATE_ID) override val certificateId: String,
    @JsonProperty(IS_FIRST_CLIENT_REQUEST) override val isFirstClientRequest: Boolean
  ) : Issued

  @JsonIgnoreProperties(ignoreUnknown = true)
  data class NotReady(@JsonProperty(REQUIRED_PERCENT) val requiredPercent: Int) : CourseCertificateIssueResponse

  @JsonIgnoreProperties(ignoreUnknown = true)
  data object Unsupported : CourseCertificateIssueResponse
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = TYPE, defaultImpl = CourseCertificationSupportResponse.Unsupported::class)
@JsonSubTypes(
  JsonSubTypes.Type(value = CourseCertificationSupportResponse.Supported::class, name = SUPPORTED),
  JsonSubTypes.Type(value = CourseCertificationSupportResponse.Unsupported::class, name = UNSUPPORTED)
)
sealed interface CourseCertificationSupportResponse {

  @JsonIgnoreProperties(ignoreUnknown = true)
  data class Supported(@JsonProperty(REQUIRED_PERCENT) val requiredPercent: Int) : CourseCertificationSupportResponse

  @JsonIgnoreProperties(ignoreUnknown = true)
  data object Unsupported : CourseCertificationSupportResponse
}
