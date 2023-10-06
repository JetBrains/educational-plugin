package com.jetbrains.edu.remote

import com.jetbrains.codeWithMe.model.projectViewModel
import com.jetbrains.rdserver.core.RemoteProjectSession

// BACKCOMPAT: 2023.2. Inline it
@Suppress("UnstableApiUsage")
fun getProjectViewModel(session: RemoteProjectSession) = session.protocol.projectViewModel