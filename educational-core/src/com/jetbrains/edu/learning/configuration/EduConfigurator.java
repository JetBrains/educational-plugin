package com.jetbrains.edu.learning.configuration;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.EmptyIcon;
import com.jetbrains.edu.coursecreator.CCUtils;
import com.jetbrains.edu.coursecreator.ui.CCNewCoursePanel;
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSynchronizer;
import com.jetbrains.edu.learning.EduCourseBuilder;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.checker.TaskCheckerProvider;
import com.jetbrains.edu.learning.checker.TheoryTaskChecker;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.ext.TaskExt;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.newproject.ui.CoursesPanel;
import kotlin.Pair;
import kotlin.collections.CollectionsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.SystemIndependent;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The main interface provides courses support for some language and course type.
 *
 * To get configurator instance for some language use {@link EduConfiguratorManager}
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
    if (file.isDirectory()) return false;
    Task task = EduUtils.getTaskForFile(project, file);
    if (task == null) {
      return false;
    }

    VirtualFile taskDir = task.getTaskDir(project);
    if (taskDir == null) {
      return false;
    }
    return ContainerUtil.find(TaskExt.findTestDirs(task, taskDir), testDir -> VfsUtilCore.isAncestor(testDir, file, true)) != null;
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
   * Allows to determine if configurator can be used to create new course using course creator features.
   *
   * @return true if configurator can be used to create new courses
   */
  default boolean isCourseCreatorEnabled() {
    return true;
  }

  /**
   * Constructs file name for Stepik tasks according to its text.
   * For example, Java requires file name should be the same as name of public class in it
   *
   * @see com.jetbrains.edu.learning.stepik.StepikTaskBuilder
   */
  @Nullable
  default String getMockFileName(@NotNull String text) {
    return null;
  }

  /**
   * Allows to customize file template used as playground in theory and choice tasks
   * Template should work along with the according {@link TheoryTaskChecker}
   *
   * @see com.jetbrains.edu.learning.stepik.StepikTaskBuilder
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

  /**
   * Allows to perform heavy computations (ex.HTTP requests) before actual project is created
   * It's recommended to perform these computations under progress
   * @throws CourseCantBeStartedException if impossible to start course
   */
  default void beforeCourseStarted(@NotNull Course course) throws CourseCantBeStartedException {

  }

  /**
   * This icon is used in places where course is associated with language.
   * For example, 'Browse Courses' and 'Create New Course' dialogs.
   *
   * @return 16x16 icon
   *
   * @see CoursesPanel
   * @see CCNewCoursePanel
   */
  @NotNull
  default Icon getLogo() {
    return EmptyIcon.ICON_16;
  }

  @Nullable
  default Pair<JPanel, String> additionalTaskTab(@Nullable Task currentTask, Project project) {
    return null;
  }
}
