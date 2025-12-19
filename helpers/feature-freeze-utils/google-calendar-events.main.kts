@file:Repository("https://repo.maven.apache.org/maven2/")
@file:DependsOn("com.auth0:java-jwt:4.4.0")
@file:DependsOn("com.squareup.okhttp3:okhttp:4.12.0")
@file:DependsOn("com.fasterxml.jackson.core:jackson-databind:2.17.2")
@file:DependsOn("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.2")
@file:Import("shared-utils.main.kts")

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Instant
import java.util.*

private val mapper = ObjectMapper().registerKotlinModule().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)


val client = OkHttpClient()

private fun parsePrivateKey(pem: String): RSAPrivateKey {
  val stripped = pem.replace("-----BEGIN PRIVATE KEY-----", "").replace("-----END PRIVATE KEY-----", "").replace("\\s".toRegex(), "")
  val keyBytes = Base64.getDecoder().decode(stripped)
  val spec = PKCS8EncodedKeySpec(keyBytes)
  return KeyFactory.getInstance("RSA").generatePrivate(spec) as RSAPrivateKey
}

data class AccessToken(@JsonProperty("access_token") val accessToken: String)

private fun getAccessToken(assertion: String): String {
  val requestBody =
    "grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer&assertion=$assertion".toRequestBody("application/x-www-form-urlencoded".toMediaType())

  val request = Request.Builder().url("https://oauth2.googleapis.com/token").post(requestBody).build()

  client.newCall(request).execute().use { response ->
    val responseBody = response.body?.string()
    if (!response.isSuccessful) {
      println("❌ Error: ${response.code} - $responseBody")
      error("Failed to get access token: $responseBody")
    }
    val json = mapper.readValue(responseBody, AccessToken::class.java)
    return json.accessToken
  }
}

data class ServiceAccountKey(@JsonProperty("private_key") val privateKey: String, @JsonProperty("client_email") val clientEmail: String)
data class EventDateTime(@JsonProperty("date") val date: String?)
data class Event(@JsonProperty("summary") val summary: String?, @JsonProperty("start") val start: EventDateTime?)
data class Events(@JsonProperty("items") val items: List<Event?>?)

fun getCalendarEvents(calendarId: String, serviceAccountKeyJson: String, startTime: Instant, endTime: Instant): List<Event?>? {
  val keyData = mapper.readValue(serviceAccountKeyJson, ServiceAccountKey::class.java)
  val clientEmail = keyData.clientEmail
  val privateKeyPem = keyData.privateKey

  val now = Instant.now()
  val privateKey = parsePrivateKey(privateKeyPem)
  val algorithm = Algorithm.RSA256(null, privateKey)

  val signedJwt = JWT.create().withIssuer(clientEmail).withSubject(clientEmail).withAudience("https://oauth2.googleapis.com/token")
    .withClaim("scope", "https://www.googleapis.com/auth/calendar.readonly").withIssuedAt(now).withExpiresAt(now.plusSeconds(3600))
    .sign(algorithm)

  val accessToken = getAccessToken(signedJwt)
  val minTime = URLEncoder.encode(startTime.toString(), "UTF-8")
  val maxTime = URLEncoder.encode(endTime.toString(), "UTF-8")
  val encodedCalendarId = URLEncoder.encode(calendarId, "UTF-8")
  val eventsUrl =
    """https://www.googleapis.com/calendar/v3/calendars/$encodedCalendarId/events?singleEvents=true&orderBy=startTime&timeMin=$minTime&timeMax=$maxTime"""

  val request = Request.Builder().url(eventsUrl).get().addHeader("Authorization", "Bearer $accessToken").build()

  val responseBody = sendRequest(client, request)
  val json = mapper.readValue(responseBody, Events::class.java)
  return json.items
}
