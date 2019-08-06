package com.jetbrains.edu.learning.codeforces.api

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

fun Long.toZonedDateTime(): ZonedDateTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(this), ZoneId.systemDefault())

fun Long.toDuration(): Duration = Duration.ofSeconds(this)