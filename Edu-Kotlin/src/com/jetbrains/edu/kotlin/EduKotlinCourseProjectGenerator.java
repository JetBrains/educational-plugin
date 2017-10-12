package com.jetbrains.edu.kotlin;

import com.intellij.facet.ui.ValidationResult;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.platform.DirectoryProjectGenerator;
import com.jetbrains.edu.coursecreator.CCUtils;
import com.jetbrains.edu.coursecreator.actions.CCCreateLesson;
import com.jetbrains.edu.coursecreator.actions.CCCreateTask;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.core.EduNames;
import com.jetbrains.edu.learning.core.EduUtils;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.intellij.generation.EduCourseModuleBuilder;
import com.jetbrains.edu.learning.intellij.generation.EduGradleModuleGenerator;
import com.jetbrains.edu.learning.intellij.generation.EduIntellijCourseProjectGeneratorBase;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.KotlinIcons;

import javax.swing.*;
import java.io.IOException;

class EduKotlinCourseProjectGenerator extends EduIntellijCourseProjectGeneratorBase {

  private static final Logger LOG = Logger.getInstance(EduKotlinCourseProjectGenerator.class);

  @Override
  protected EduCourseModuleBuilder studyModuleBuilder() {
    return new EduKotlinKoansModuleBuilder(myCourse);
  }

  @Nullable
  @Override
  protected Icon getLogo() {
    return KotlinIcons.SMALL_LOGO;
  }

  @NotNull
  @Override
  public DirectoryProjectGenerator getDirectoryProjectGenerator() {
    return EduUtils.isAndroidStudio() ? new KoansAndroidProjectGenerator() : super.getDirectoryProjectGenerator();
  }

  private class KoansAndroidProjectGenerator implements DirectoryProjectGenerator {
    @Nls
    @NotNull
    @Override
    public String getName() {
      return "";
    }

    @Nullable
    @Override
    public Icon getLogo() {
      return EduKotlinCourseProjectGenerator.this.getLogo();
    }

    @Override
    public void generateProject(@NotNull Project project, @NotNull VirtualFile baseDir, @Nullable Object o, @NotNull Module module) {
      ApplicationManager.getApplication().runWriteAction(() -> {
        try {
          StudyTaskManager.getInstance(project).setCourse(myCourse);
          if (CCUtils.isCourseCreator(project)) {
            Lesson lesson = (Lesson)new CCCreateLesson().createAndInitItem(myCourse, null, EduNames.LESSON + 1, 1);
            Task task = (Task)new CCCreateTask().createAndInitItem(myCourse, lesson, EduNames.TASK + 1, 1);
            lesson.addTask(task);
            myCourse.getLessons(true).add(lesson);
            initTask(task);
          }
          myCourse.initCourse(false);
          EduGradleModuleGenerator.createCourseContent(project, myCourse, baseDir.getPath());
        } catch (IOException e) {
          LOG.error("Failed to generate course", e);
        }
      });
    }

    @NotNull
    @Override
    public ValidationResult validate(@NotNull String s) {
      return ValidationResult.OK;
    }
  }

  static void initTask(@NotNull Task task) {
    TaskFile taskFile = new TaskFile();
    taskFile.setTask(task);
    taskFile.name  = EduKotlinPluginConfigurator.TASK_KT;
    taskFile.text = EduUtils.getTextFromInternalTemplate(EduKotlinPluginConfigurator.TASK_KT);
    task.addTaskFile(taskFile);
    task.getTestsText().put(EduKotlinPluginConfigurator.TESTS_KT, EduUtils.getTextFromInternalTemplate(EduKotlinPluginConfigurator.TESTS_KT));
  }
}
