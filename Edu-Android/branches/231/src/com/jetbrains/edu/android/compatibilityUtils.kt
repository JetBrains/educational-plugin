package com.jetbrains.edu.android

import com.android.sdklib.internal.avd.AvdInfo
import com.android.tools.idea.avdmanager.AvdOptionsModel
import com.intellij.util.containers.orNull

// BACKCOMPAT: 2022.3. Inline it
fun AvdOptionsModel.getAvd(): AvdInfo? = createdAvd.orNull()
