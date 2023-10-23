package com.jetbrains.edu.remote

import com.intellij.openapi.client.ClientProjectSession
import com.jetbrains.codeWithMe.model.ProjectViewModel
import com.jetbrains.codeWithMe.model.projectViewModel
import com.jetbrains.rd.platform.client.ProtocolProjectSession

// BACKCOMPAT: 2023.2. Inline it
@Suppress("UnstableApiUsage")
fun getProjectViewModel(session: ClientProjectSession): ProjectViewModel = (session as ProtocolProjectSession).protocol.projectViewModel