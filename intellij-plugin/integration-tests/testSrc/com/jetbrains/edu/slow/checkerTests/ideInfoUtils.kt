package com.jetbrains.edu.slow.checkerTests

import com.intellij.ide.starter.models.IdeInfo
import com.intellij.tools.ide.starter.product.idea.ultimate.IdeaUltimate
import com.intellij.tools.ide.starter.product.pycharm.PyCharm

fun ideaUltimate(): IdeInfo = IdeInfo.IdeaUltimate.copy(
  version = IdeaUltimateVersionInfo.VERSION,
  buildType = IdeaUltimateVersionInfo.BUILD_TYPE.type
)

fun pyCharm(): IdeInfo = IdeInfo.PyCharm.copy(
  version = PyCharmVersionInfo.VERSION,
  buildType = PyCharmVersionInfo.BUILD_TYPE.type
)