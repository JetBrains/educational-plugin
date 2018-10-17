package com.jetbrains.edu.learning;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.coursecreator.CCUtils;
import com.jetbrains.edu.coursecreator.configuration.YamlFormatSynchronizer;
import com.jetbrains.edu.learning.checker.TaskCheckerProvider;
import com.jetbrains.edu.learning.checker.TheoryTaskChecker;
import kotlin.collections.CollectionsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.SystemIndependent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The main interface provides courses support for some language.
 *
 * To get configurator instance for some language use {@link EduConfiguratorManager}
 * instead of {@link com.intellij.lang.LanguageExtension} because configurator shouldn't be used in some environments
 * and {@link EduConfiguratorManager} supports the corresponding filtering.
 *
 * @param <Settings> container type holds course project settings state
 *
 * @see EduConfiguratorManager
 */
public interface EduConfigurator<Settings> {
  String EP_NAME = "Educational.configurator";

  Logger LOG = Logger.getInstance(EduConfigurator.class);

  @NotNull
  EduCourseBuilder<Settings> getCourseBuilder();

  @NotNull
  String getTestFileName();

  /**
   * Used in educator plugin to filter files to be packed into course archive
   */
  default boolean excludeFromArchive(@NotNull Project project, @NotNull VirtualFile file) {
    List<String> ancestorNames = new ArrayList<>();
    VirtualFile parent = file;
    while (parent != null) {
      ancestorNames.add(parent.getName());
      parent = parent.getParent();
    }
    String name = file.getName();

    // Project related files
    if (ancestorNames.contains(Project.DIRECTORY_STORE_FOLDER) || "iml".equals(file.getExtension())) return true;
    // Course structure files
    if (EduUtils.isTaskDescriptionFile(name) || YamlFormatSynchronizer.isConfigFile(file)) return true;
    // Hidden files
    if (CollectionsKt.any(ancestorNames, ancestorName -> ancestorName.startsWith("."))) return true;
    // Special files
    if (ancestorNames.contains(CCUtils.GENERATED_FILES_FOLDER) || EduNames.HINTS.equals(name) || EduNames.STEPIK_IDS_JSON.equals(name)) return true;

    return false;
  }

  /**
   * @return true for all the test files
   */
  default boolean isTestFile(@NotNull Project project, @NotNull VirtualFile file) {
    return false;
  }

  /**
   * Provides directory path where task files should be placed in task folder.
   * Can be empty.
   *
   * For example, task folder is `courseName/lesson1/task1` and `getSourceDir` returns `src`
   * then any task files should be placed in `courseName/lesson1/task1/src` folder
   *
   * @return task files directory path
   */
  @SystemIndependent
  @NotNull
  default String getSourceDir() {
    return "";
  }

  /**
   * Provides list of directories where test files should be placed in task folder.
   * Can be empty.
   *
   * See {@link EduConfigurator#getSourceDir()} javadoc for example.
   *
   * @return list of test files directories paths
   */
  @SystemIndependent
  @NotNull
  default List<String> getTestDirs() {
    return Collections.emptyList();
  }

  /**
   * @return class that provide checkers for all types of tasks
   */
  @NotNull TaskCheckerProvider getTaskCheckerProvider();

  /**
   * Allows to determine if configurator can be used in current environment or not.
   *
   * @return true if configurator can be used, false otherwise
   *
   * @see EduConfiguratorManager
   */
  default boolean isEnabled() {
    return true;
  }

  /**
   * Allows to customize file template used as playground in theory and choice tasks
   * Template should work along with the according {@link TheoryTaskChecker}
   *
   * @see StepikTaskBuilder
   *
   */
  default String getMockTemplate() {
    return "";
  }

  /**
   * Provide IDE plugin ids which are required for correct work of courses for the corresponding language.
   *
   * @return list of plugin ids
   */
  default List<String> pluginRequirements() {
    return Collections.emptyList();
  }
}
