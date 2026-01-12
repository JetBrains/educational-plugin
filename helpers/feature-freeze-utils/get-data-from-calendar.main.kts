#!/usr/bin/env kotlin

@file:Repository("https://repo.maven.apache.org/maven2/")
@file:DependsOn("com.auth0:java-jwt:4.4.0")
@file:DependsOn("com.google.code.gson:gson:2.10.1")
@file:Import("google-calendar-events.main.kts")

import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.format.TextStyle
import java.util.Locale
import kotlin.system.exitProcess

val calendarId = System.getenv("GOOGLE_CALENDAR_ID") ?: error("Missing GOOGLE_CALENDAR_ID environment variable")
val serviceAccountKeyJson = System.getenv("GCP_SERVICE_ACCOUNT_KEY") ?: error("Missing GCP_SERVICE_ACCOUNT_KEY environment variable")

val today: LocalDate = LocalDate.now()

val events: List<Event?>? = getCalendarEvents(calendarId, serviceAccountKeyJson, Instant.now(), Instant.now().plus(21, ChronoUnit.DAYS))

fun getEventDate(eventsList: List<Event?>?, keyword: String): LocalDate? = eventsList?.firstOrNull { item: Event? ->
  val summary = item?.summary ?: ""
  summary.contains(keyword, ignoreCase = true)
}?.start?.date?.let { LocalDate.parse(it) }

private fun toWeekDay(date: LocalDate?): String? = date?.dayOfWeek?.getDisplayName(TextStyle.FULL, Locale.ENGLISH)

// No feature freeze if the corresponding events are not found
val featureFreezeDate = getEventDate(events, "Feature Freeze") ?: exitProcess(0)

if (featureFreezeDate.isEqual(today)) {
  println("##teamcity[setParameter name='feature.freeze.date' value='$featureFreezeDate (${toWeekDay(featureFreezeDate)})']")
  println("##teamcity[setParameter name='feature.freeze.today' value='true']")
}
else if (featureFreezeDate.isBefore(today.plus(7, ChronoUnit.DAYS))) {
  val releaseBuildsDate = getEventDate(events, "Release Builds")
  val releaseDate = getEventDate(events, "JetBrains Academy Plugin Release")

  println("##teamcity[setParameter name='current.date' value='$today (Today)']")
  println("##teamcity[setParameter name='current.dayOfWeek' value='${toWeekDay(today)}']")
  println("##teamcity[setParameter name='feature.freeze.date' value='$featureFreezeDate (${toWeekDay(featureFreezeDate)})']")
  println("##teamcity[setParameter name='release.builds.date' value='$releaseBuildsDate (${toWeekDay(releaseBuildsDate)})']")
  println("##teamcity[setParameter name='plugin.release.date' value='$releaseDate (${toWeekDay(releaseDate)})']")

  println("Feature freeze incoming")
  println("##teamcity[setParameter name='feature.freeze.incoming' value='true']")
}
