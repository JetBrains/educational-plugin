package com.jetbrains.edu.android

import com.android.sdklib.internal.avd.AvdInfo
import com.android.tools.idea.avdmanager.AvdOptionsModel

typealias GoogleMavenArtifactId = com.android.tools.idea.projectsystem.GoogleMavenArtifactId

@Suppress("RedundantNullableReturnType")
fun AvdOptionsModel.getAvd(): AvdInfo? = createdAvd
