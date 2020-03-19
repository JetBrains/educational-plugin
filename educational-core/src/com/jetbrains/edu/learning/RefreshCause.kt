package com.jetbrains.edu.learning

enum class RefreshCause {
  PROJECT_CREATED,
  STRUCTURE_MODIFIED,
  DEPENDENCIES_UPDATED // might be build.gradle or requirements.txt
}
