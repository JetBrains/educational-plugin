@file:Repository("https://repo.maven.apache.org/maven2/")
@file:DependsOn("com.auth0:java-jwt:4.4.0")
@file:DependsOn("com.google.code.gson:gson:2.10.1")

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Instant
import java.util.Base64
import java.util.Date

private val gson = Gson()

private fun parsePrivateKey(pem: String): RSAPrivateKey {
    val stripped = pem
        .replace("-----BEGIN PRIVATE KEY-----", "")
        .replace("-----END PRIVATE KEY-----", "")
        .replace("\\s".toRegex(), "")
    val keyBytes = Base64.getDecoder().decode(stripped)
    val spec = PKCS8EncodedKeySpec(keyBytes)
    return KeyFactory.getInstance("RSA").generatePrivate(spec) as RSAPrivateKey
}

private fun getAccessToken(assertion: String): String {
    val url = URL("https://oauth2.googleapis.com/token")
    val conn = url.openConnection() as HttpURLConnection
    conn.requestMethod = "POST"
    conn.doOutput = true
    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

    val body = "grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer&assertion=" + assertion
    conn.outputStream.use { it.write(body.toByteArray()) }

    if (conn.responseCode != 200) {
        val errBody = conn.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
        error("Failed to get access token: " + errBody)
    }

    val response = conn.inputStream.bufferedReader().readText()
    return gson.fromJson(response, JsonObject::class.java)
        .get("access_token").asString
}

fun getCalendarEvents(calendarId: String, serviceAccountKeyJson: String, startTime: Instant, endTime: Instant): JsonArray {

    val keyData = gson.fromJson(serviceAccountKeyJson, JsonObject::class.java)
    val clientEmail = keyData.get("client_email").asString
    val privateKeyPem = keyData.get("private_key").asString

    val now = Instant.now()
    val privateKey = parsePrivateKey(privateKeyPem)
    val algorithm = Algorithm.RSA256(null, privateKey)

    val signedJwt = JWT.create()
        .withIssuer(clientEmail)
        .withSubject(clientEmail)
        .withAudience("https://oauth2.googleapis.com/token")
        .withClaim("scope", "https://www.googleapis.com/auth/calendar.readonly")
        .withIssuedAt(Date.from(now))
        .withExpiresAt(Date.from(now.plusSeconds(3600)))
        .sign(algorithm)

    val accessToken = getAccessToken(signedJwt)
    val minTime = URLEncoder.encode(startTime.toString(), "UTF-8")
    val maxTime = URLEncoder.encode(endTime.toString(), "UTF-8")

    val encodedCalendarId = URLEncoder.encode(calendarId, "UTF-8")
    val eventsUrl =
        """https://www.googleapis.com/calendar/v3/calendars/$encodedCalendarId/events?singleEvents=true&orderBy=startTime&timeMin=$minTime&timeMax=$maxTime"""

    val conn = URL(eventsUrl).openConnection() as HttpURLConnection
    conn.setRequestProperty("Authorization", "Bearer " + accessToken)

    if (conn.responseCode !in 200..299) {
        val errMsg = conn.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
        error("Failed to fetch events (HTTP " + conn.responseCode + "): " + errMsg)
    }

    val events = conn.inputStream.bufferedReader().readText()
    val eventsJson = gson.fromJson(events, JsonObject::class.java)
    return eventsJson.getAsJsonArray("items")
}
