package com.jetbrains.edu.learning;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.courseFormat.tasks.TaskWithSubtasks;
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.IOException;

/**
 * The main interface provides courses creation for some language.
 *
 * @param <Settings> container type holds course project settings state
 */
public interface EduCourseBuilder<Settings> {

  Logger LOG = Logger.getInstance(EduCourseBuilder.class);

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

  default void createTestsForNewSubtask(@NotNull Project project, @NotNull TaskWithSubtasks task) {
  }

  /**
   * @return object responsible for language settings
   * @see LanguageSettings
   */
  @NotNull
  LanguageSettings<Settings> getLanguageSettings();

  @Nullable
  default CourseProjectGenerator<Settings> getCourseProjectGenerator(@NotNull Course course) {
    return null;
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
