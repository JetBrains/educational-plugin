package com.jetbrains.edu.remote

import com.intellij.openapi.client.ClientProjectSession
import com.jetbrains.codeWithMe.model.ProjectViewModel
import com.jetbrains.codeWithMe.model.projectViewModel
import com.jetbrains.rdserver.core.protocolModel

// BACKCOMPAT: 2023.2. Inline it
@Suppress("UnstableApiUsage")
typealias ProjectSession = ClientProjectSession

// BACKCOMPAT: 2023.2. Inline it
@Suppress("UnstableApiUsage")
fun getProjectViewModel(session: ProjectSession): ProjectViewModel = session.protocolModel.projectViewModel