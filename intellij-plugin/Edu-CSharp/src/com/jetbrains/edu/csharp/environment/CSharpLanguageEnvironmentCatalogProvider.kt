package com.jetbrains.edu.csharp.environment

import com.intellij.openapi.util.UserDataHolder
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.Result
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.environment.EnvironmentUiKind
import com.jetbrains.edu.learning.newproject.environment.LanguageEnvironmentCatalog
import com.jetbrains.edu.learning.newproject.environment.LanguageEnvironmentCatalogProvider

object CSharpLanguageEnvironmentCatalogProvider : LanguageEnvironmentCatalogProvider<CSharpLanguageEnvironment> {
  override val uiKind: EnvironmentUiKind
    get() = EnvironmentUiKind.Empty

  override suspend fun default(): Result<CSharpLanguageEnvironment, String> = Ok(CSharpLanguageEnvironment)

  context(_: UserDataHolder)
  override suspend fun collectEnvironmentsForCourse(
    course: Course
  ): Result<LanguageEnvironmentCatalog<CSharpLanguageEnvironment>, String> = Ok(LanguageEnvironmentCatalog(CSharpLanguageEnvironment))
}
