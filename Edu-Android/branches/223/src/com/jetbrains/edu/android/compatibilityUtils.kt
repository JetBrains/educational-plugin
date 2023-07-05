package com.jetbrains.edu.android

import com.android.sdklib.internal.avd.AvdInfo
import com.android.tools.idea.avdmanager.AvdOptionsModel

@Suppress("RedundantNullableReturnType")
fun AvdOptionsModel.getAvd(): AvdInfo? = createdAvd