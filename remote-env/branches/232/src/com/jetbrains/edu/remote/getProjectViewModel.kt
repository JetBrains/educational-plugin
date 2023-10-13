package com.jetbrains.edu.remote

import com.jetbrains.codeWithMe.model.ProjectViewModel
import com.jetbrains.codeWithMe.model.projectViewModel
import com.jetbrains.rd.platform.client.ProtocolProjectSession

// BACKCOMPAT: 2023.2. Inline it
@Suppress("UnstableApiUsage")
fun getProjectViewModel(session: ProtocolProjectSession): ProjectViewModel = session.protocol.projectViewModel