package com.jetbrains.edu.slow.checkerTests

import com.intellij.ide.starter.community.model.BuildType
import com.intellij.ide.starter.ide.IdeProductProvider
import com.intellij.ide.starter.models.IdeInfo

fun ideaUltimate(): IdeInfo = IdeProductProvider.IU.copy(version = "2026.1.1", buildType = BuildType.RELEASE.type)