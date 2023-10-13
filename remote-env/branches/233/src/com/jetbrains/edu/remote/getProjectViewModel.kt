package com.jetbrains.edu.remote

import com.jetbrains.codeWithMe.model.ProjectViewModel
import com.jetbrains.codeWithMe.model.projectViewModel
import com.jetbrains.rd.platform.client.ProtocolProjectSession

// BACKCOMPAT: 2023.2. Inline it
fun getProjectViewModel(session: ProtocolProjectSession): ProjectViewModel = session.solution.projectViewModel