package com.jetbrains.edu.remote

import com.jetbrains.codeWithMe.model.ProjectViewModel
import com.jetbrains.codeWithMe.model.projectViewModel
import com.jetbrains.rdserver.core.RemoteProjectSession

// BACKCOMPAT: 2023.2. Inline it
@Suppress("UnstableApiUsage")
typealias ProjectSession = RemoteProjectSession

// BACKCOMPAT: 2023.2. Inline it
@Suppress("UnstableApiUsage")
fun getProjectViewModel(session: ProjectSession): ProjectViewModel = session.protocol.projectViewModel