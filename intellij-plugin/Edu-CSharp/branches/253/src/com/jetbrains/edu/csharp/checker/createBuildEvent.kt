package com.jetbrains.edu.csharp.checker

import com.jetbrains.rider.build.BuildEventsService
import com.jetbrains.rider.model.build.BuildEvent

fun BuildEventsService.getBuildEvent(offset: Long): BuildEvent = getEvent(offset)