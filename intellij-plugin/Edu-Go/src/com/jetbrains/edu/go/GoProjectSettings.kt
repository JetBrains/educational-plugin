package com.jetbrains.edu.go

import com.goide.sdk.GoSdk
import com.jetbrains.edu.learning.newproject.EduProjectSettings

data class GoProjectSettings(val sdk: GoSdk) : EduProjectSettings
