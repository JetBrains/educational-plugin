package com.jetbrains.edu.slow.checkerTests

import com.intellij.ide.starter.community.model.BuildType
import com.intellij.ide.starter.models.IdeInfo
import com.intellij.tools.ide.starter.product.idea.ultimate.IdeaUltimate
import com.intellij.tools.ide.starter.product.pycharm.PyCharm

fun ideaUltimate(): IdeInfo = IdeInfo.IdeaUltimate.copy(version = "2026.2.0.1", buildType = BuildType.RELEASE.type)

fun pyCharm(): IdeInfo = IdeInfo.PyCharm.copy(version = "2026.2", buildType = BuildType.RELEASE.type)