package com.jetbrains.edu.learning.intellij.generation;

import com.intellij.ide.util.newProjectWizard.AbstractProjectWizard;
import com.intellij.ide.util.newProjectWizard.StepSequence;
import com.intellij.ide.util.projectWizard.ModuleBuilder;
import com.intellij.ide.util.projectWizard.ProjectBuilder;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleWithNameAlreadyExists;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.actions.NewModuleAction;
import com.intellij.openapi.util.InvalidDataException;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.intellij.EduIntellijUtils;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;

public class EduModuleBuilderUtils {

  private EduModuleBuilderUtils() {
  }

  public static void createCourseModuleContent(@NotNull ModifiableModuleModel moduleModel, @NotNull Project project, Course course, String moduleDir)
    throws IOException, ModuleWithNameAlreadyExists, JDOMException, ConfigurationException {
    if (moduleDir == null) {
      return;
    }
    UtilModuleBuilder utilModuleBuilder = new UtilModuleBuilder(moduleDir, course.getAdditionalMaterialsTask());
    Module utilModule = utilModuleBuilder.createAndCommitIfNeeded(project, moduleModel, false);
    createLessonModules(project, moduleModel, course, moduleDir, utilModule);
    moduleModel.commit();
  }


  private static void createLessonModules(@NotNull Project project, @NotNull ModifiableModuleModel moduleModel, Course course, String moduleDir, Module utilModule)
    throws InvalidDataException, IOException, ModuleWithNameAlreadyExists, JDOMException, ConfigurationException {
    List<Lesson> lessons = course.getLessons();
    for (int i = 0; i < lessons.size(); i++) {
      int lessonVisibleIndex = i + 1;
      Lesson lesson = lessons.get(i);
      lesson.setIndex(lessonVisibleIndex);
      LessonModuleBuilder lessonModuleBuilder = new LessonModuleBuilder(moduleDir, lesson, utilModule);
      lessonModuleBuilder.createAndCommitIfNeeded(project, moduleModel, false);
    }
  }

  private static void updateAdaptiveCourseTaskFileNames(@NotNull Project project, @NotNull Course course) {
    if (course.isAdaptive()) {
      Lesson adaptiveLesson = course.getLessons().get(0);
      Task task = adaptiveLesson.getTaskList().get(0);
      for (TaskFile taskFile : task.getTaskFiles().values()) {
        EduIntellijUtils.nameTaskFileAfterContainingClass(task, taskFile, project);
      }
    }
  }

  @Nullable
  public static Module createModule(@NotNull Project project, @NotNull ModuleBuilder moduleBuilder,
                                    @NotNull String defaultPath) {
    AbstractProjectWizard projectWizard = new AbstractProjectWizard("", project, defaultPath) {

      @Override
      public StepSequence getSequence() {
        return null;
      }

      @Override
      public ProjectBuilder getProjectBuilder() {
        return moduleBuilder;
      }
    };
    return new NewModuleAction().createModuleFromWizard(project, null, projectWizard);
  }
}
