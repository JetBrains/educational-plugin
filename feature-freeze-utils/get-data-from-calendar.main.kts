#!/usr/bin/env kotlin

@file:Repository("https://repo.maven.apache.org/maven2/")
@file:DependsOn("com.auth0:java-jwt:4.4.0")
@file:DependsOn("com.google.code.gson:gson:2.10.1")
@file:Import("google-calendar-events.main.kts")

import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import java.util.Locale

val calendarId = System.getenv("GOOGLE_CALENDAR_ID")
    ?: error("Missing GOOGLE_CALENDAR_ID environment variable")
val serviceAccountKeyJson = System.getenv("GCP_SERVICE_ACCOUNT_KEY")
    ?: error("Missing GCP_SERVICE_ACCOUNT_KEY environment variable")

val today: LocalDate = LocalDate.now()

val events: JsonArray = getCalendarEvents(calendarId, serviceAccountKeyJson, Instant.now(), Instant.now().plus(21, ChronoUnit.DAYS))

fun getEventDate(keyword: String): LocalDate? = events.filter { item: JsonElement ->
        val event = item.asJsonObject
        val summary = event.get("summary")?.asString ?: ""
        summary.contains(keyword, ignoreCase = true)
    }.firstOrNull()?.asJsonObject?.get("start")?.asJsonObject?.get("date")?.asString?.let { LocalDate.parse(it) }

fun capitalizeDayOfWeek(date: LocalDate?): String = date?.dayOfWeek.toString()
    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

val featureFreezeDate = getEventDate("Feature Freeze")

if (featureFreezeDate != null && featureFreezeDate.isEqual(LocalDate.now())) {
    println(
        "##teamcity[setParameter name='feature.freeze.date' value='$featureFreezeDate (${
            capitalizeDayOfWeek(
                featureFreezeDate
            )
        })']"
    )
    println("##teamcity[setParameter name='feature.freeze.today' value='true']")
} else if ((featureFreezeDate != null) && featureFreezeDate.isBefore(today.plus(7, ChronoUnit.DAYS))
) {
    val releaseBuildsDate = getEventDate("Release Builds")
    val releaseDate = getEventDate("JetBrains Academy Plugin Release")

    println("##teamcity[setParameter name='current.date' value='$today (Today)']")
    println(
        "##teamcity[setParameter name='feature.freeze.date' value='$featureFreezeDate (${
            capitalizeDayOfWeek(
                featureFreezeDate
            )
        })']"
    )
    println(
        "##teamcity[setParameter name='release.builds.date' value='$releaseBuildsDate (${
            capitalizeDayOfWeek(
                releaseBuildsDate
            )
        })']"
    )
    println("##teamcity[setParameter name='plugin.release.date' value='$releaseDate (${capitalizeDayOfWeek(releaseDate)})']")

    println("Feature freeze incoming")
    println("##teamcity[setParameter name='feature.freeze.incoming' value='true']")
}
