package com.jetbrains.edu.learning;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.StdModuleTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.learning.actions.CompareWithAnswerAction;
import com.jetbrains.edu.learning.actions.*;
import com.jetbrains.edu.learning.checker.TaskChecker;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.courseFormat.tasks.TaskWithSubtasks;
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils;
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

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
  String getTestFileName();

  /**
   * Default language name used to publish submissions to stepik
   * @return
   */
  @NotNull
  default String getStepikDefaultLanguage() {
    return "";
  }

  /**
   * Creates content (including its directory or module) of new lesson in project
   *
   * @param project Parameter is used in Java and Kotlin plugins
   * @param lesson  Lesson to create content for. It's already properly initialized and added to course.
   * @return VirtualFile of created lesson
   */
  default VirtualFile createLessonContent(@NotNull Project project,
                                          @NotNull Lesson lesson,
                                          @NotNull VirtualFile parentDirectory) {
    final VirtualFile[] lessonDirectory = new VirtualFile[1];
    ApplicationManager.getApplication().runWriteAction(() -> {
      String lessonDirName = EduNames.LESSON + lesson.getIndex();
      try {
        lessonDirectory[0] = VfsUtil.createDirectoryIfMissing(parentDirectory, lessonDirName);
      } catch (IOException e) {
        LOG.error("Failed to create lesson directory", e);
      }
    });
    return lessonDirectory[0];
  }

  /**
   * Creates content (including its directory or module) of new task in project
   *
   * @param task Task to create content for. It's already properly initialized and added to corresponding lesson.
   * @return VirtualFile of created task
   */
  VirtualFile createTaskContent(@NotNull final Project project,
                                @NotNull final Task task,
                                @NotNull final VirtualFile parentDirectory,
                                @NotNull final Course course);

  /**
   * Used in educator plugin to filter files to be packed into course archive
   */
  boolean excludeFromArchive(@NotNull String name);

  /**
   * @return true for all the test files including tests for subtasks
   */
  default boolean isTestFile(VirtualFile file) {
    return false;
  }

  default void createTestsForNewSubtask(@NotNull Project project, @NotNull TaskWithSubtasks task) {
  }

  @NotNull
  TaskChecker<EduTask> getEduTaskChecker(@NotNull EduTask task, @NotNull Project project);

  @NotNull
  default DefaultActionGroup getTaskDescriptionActionGroup() {
    final DefaultActionGroup group = new DefaultActionGroup();
    String[] ids = new String[]{
      CheckAction.ACTION_ID,
      PreviousTaskAction.ACTION_ID,
      NextTaskAction.ACTION_ID,
      RefreshTaskFileAction.ACTION_ID,
      ShowHintAction.ACTION_ID,
      CompareWithAnswerAction.ACTION_ID
    };
    ActionManager actionManager = ActionManager.getInstance();
    Arrays.stream(ids)
      .map(actionManager::getAction)
      .filter(Objects::nonNull)
      .forEach(group::add);

    group.add(new EditInputAction());
    return group;
  }

  /**
   * Configures (adds libraries for example) task module for languages that require modules
   * <br>
   * Example in <a href="https://github.com/JetBrains/educational-plugins/tree/master/Edu-Kotlin">Edu Kotlin</a> plugin
   */
  default void configureModule(@NotNull Module module) {
  }

  /**
   * Creates module structure for given course
   * <br>
   * Example in <a href="https://github.com/JetBrains/educational-plugins/tree/master/Edu-Kotlin">Edu Kotlin</a> plugin
   */
  default void createCourseModuleContent(@NotNull ModifiableModuleModel moduleModel,
                                         @NotNull Project project,
                                         @NotNull Course course,
                                         @Nullable String moduleDir) {
    GeneratorUtils.createCourse(course, project.getBaseDir());
  }

  default List<String> getBundledCoursePaths() {
    return Collections.emptyList();
  }

  /**
   * @return object responsible for language settings
   * @see LanguageSettings
   */
  @NotNull
  LanguageSettings<Settings> getLanguageSettings();

  @Nullable
  default CourseProjectGenerator<Settings> getEduCourseProjectGenerator(@NotNull Course course) {
    return null;
  }

  default ModuleType getModuleType() {
    return StdModuleTypes.JAVA;
  }

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
   * Main interface responsible for course project language settings such as JDK or interpreter
   *
   * @param <Settings> container type holds project settings state
   */
  interface LanguageSettings<Settings> {

    /**
     * Returns UI component that allows user to select course project settings such as project JDK or interpreter.
     *
     * @param course course of creating project
     * @return UI component with project settings. Can be null
     */
    @Nullable
    default LabeledComponent<JComponent> getLanguageSettingsComponent(@NotNull Course course) {
      return null;
    }

    /**
     * Returns project settings associated with state of language settings UI component.
     * It should be passed into project generator to set chosen settings in course project.
     *
     * @return project settings object
     */
    @NotNull
    Settings getSettings();
  }
}
