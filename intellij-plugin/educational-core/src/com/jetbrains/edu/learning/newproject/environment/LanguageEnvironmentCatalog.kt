package com.jetbrains.edu.learning.newproject.environment

/**
 * A set of all possible [LanguageEnvironment<T>] for some course.
 * One of them is marked as recommended.
 *
 * It is expected that a user needs to select one of the environments from the entire catalog.
 * The UI depends on the structure of the catalog.
 * Currently, there are only two types of catalogs and corresponding UIs:
 * 1. The catalog is linear, and the UI is a dropdown list to select one of the environments.
 * 2. The catalog represents a single environment, and the UI is empty because there is no need to select it. (C#, C++)
 *
 * The examples of linear catalogs are: a list of all suitable JVMs, or a list of all suitable Python interpreters.
 * If at some moment we need a multidimensional environment space - for example, a course needs
 * simultaneously some JVM + some Python interpreter, the [LanguageEnvironmentCatalog] class should be modified.
 */
data class LanguageEnvironmentCatalog<out E: LanguageEnvironment>(
  /**
   * The recommended environment. Must be present in the [environments] list.
   */
  val recommended: E,

  /**
   * The list of all environments to be chosen from. The list must be non-empty.
   */
  val environments: List<E>
)