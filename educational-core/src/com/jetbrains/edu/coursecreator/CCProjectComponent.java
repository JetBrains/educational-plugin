package com.jetbrains.edu.coursecreator;

import com.intellij.ide.util.projectWizard.ModuleBuilder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.actions.ModuleDeleteProvider;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.jetbrains.edu.learning.*;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import com.jetbrains.edu.learning.courseFormat.ext.CourseExt;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.intellij.GradleCourseBuilderBase;
import com.jetbrains.edu.learning.intellij.generation.EduGradleModuleGenerator;
import com.jetbrains.edu.learning.statistics.EduUsagesCollector;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CCProjectComponent extends AbstractProjectComponent {
  private static final Logger LOG = Logger.getInstance(CCProjectComponent.class);

  private CCVirtualFileListener myTaskFileLifeListener;
  private final Project myProject;

  protected CCProjectComponent(Project project) {
    super(project);
    myProject = project;
  }

  public void migrateIfNeeded() {
    Course studyCourse = StudyTaskManager.getInstance(myProject).getCourse();
    if (studyCourse == null) {
      Course oldCourse = CCProjectService.getInstance(myProject).getCourse();
      if (oldCourse == null) {
        return;
      }
      StudyTaskManager.getInstance(myProject).setCourse(oldCourse);
      CCProjectService.getInstance(myProject).setCourse(null);
      oldCourse.initCourse(true);
      oldCourse.setCourseMode(CCUtils.COURSE_MODE);
      transformFiles(oldCourse, myProject);
    } else {
      EduConfigurator<?> configurator = EduConfiguratorManager.forLanguage(studyCourse.getLanguageById());
      if (configurator == null) return;
      EduCourseBuilder<?> courseBuilder = configurator.getCourseBuilder();
      if (courseBuilder instanceof GradleCourseBuilderBase && !EduUtils.isConfiguredWithGradle(myProject)) {
        convertToGradleProject(studyCourse, ((GradleCourseBuilderBase) courseBuilder).getBuildGradleTemplateName());
      }
    }
  }

  private void convertToGradleProject(@NotNull Course course, @NotNull String buildGradleTemplateName) {
    String basePath = myProject.getBasePath();
    if (basePath == null) {
      return;
    }

    ModuleManager moduleManager = ModuleManager.getInstance(myProject);
    Module[] modules = moduleManager.getModules();
    final ModifiableModuleModel modifiableModuleModel = moduleManager.getModifiableModel();
    for (Module module : modules) {
      ModuleDeleteProvider.removeModule(module, Collections.emptyList(), modifiableModuleModel);
      ModuleBuilder.deleteModuleFile(module.getModuleFilePath());
    }

    ApplicationManager.getApplication().runWriteAction(() -> {
      modifiableModuleModel.commit();

      try {
        EduGradleModuleGenerator.createProjectGradleFiles(basePath, myProject.getName(), buildGradleTemplateName);

        StartupManager.getInstance(myProject).runWhenProjectIsInitialized(() -> transformCourseStructure(course, myProject));

      } catch (IOException e) {
        LOG.error(e);
      }
    });

    EduProjectComponent.setGradleSettings(basePath, myProject);
  }

  private static void transformFiles(Course course, Project project) {
    List<VirtualFile> files = getAllAnswerTaskFiles(course, project);
    for (VirtualFile answerFile : files) {
      ApplicationManager.getApplication().runWriteAction(() -> {
        String answerName = answerFile.getName();
        String nameWithoutExtension = FileUtil.getNameWithoutExtension(answerName);
        String name = FileUtil.getNameWithoutExtension(nameWithoutExtension) + "." + FileUtilRt.getExtension(answerName);
        VirtualFile parent = answerFile.getParent();
        VirtualFile file = parent.findChild(name);
        try {
          if (file != null) {
            file.delete(CCProjectComponent.class);
          }
          VirtualFile windowsDescrFile = parent.findChild(FileUtil.getNameWithoutExtension(name) + EduNames.WINDOWS_POSTFIX);
          if (windowsDescrFile != null) {
            windowsDescrFile.delete(CCProjectComponent.class);
          }
          answerFile.rename(CCProjectComponent.class, name);
        }
        catch (IOException e) {
          LOG.error(e);
        }
      });
    }
  }

  @NotNull
  private static List<VirtualFile> getAllAnswerTaskFiles(@NotNull Course course, @NotNull Project project) {
    String sourceDir = CourseExt.getSourceDir(course);
    if (sourceDir == null) return Collections.emptyList();

    List<VirtualFile> result = new ArrayList<>();
    for (Lesson lesson : course.getLessons()) {
      for (Task task : lesson.getTaskList()) {
        for (Map.Entry<String, TaskFile> entry : task.getTaskFiles().entrySet()) {
          String name = entry.getKey();
          String answerName = FileUtil.getNameWithoutExtension(name) + CCUtils.ANSWER_EXTENSION_DOTTED + FileUtilRt.getExtension(name);

          String taskPath = FileUtil.join(project.getBasePath(), EduNames.LESSON + lesson.getIndex(), EduNames.TASK + task.getIndex());
          if (!sourceDir.isEmpty()) {
            taskPath = FileUtil.join(taskPath, sourceDir);
          }

          VirtualFile taskFile = LocalFileSystem.getInstance().findFileByPath(FileUtil.join(taskPath, answerName));
          if (taskFile != null) {
            result.add(taskFile);
          }
        }
      }
    }
    return result;
  }

  private static void transformCourseStructure(Course course, Project project) {
    List<VirtualFile> files = getAllTestFiles(course, project);
    for (VirtualFile testFile : files) {
      ApplicationManager.getApplication().runWriteAction(() -> {
        VirtualFile parent = testFile.getParent().getParent();
        try {
          VirtualFile testDir = parent.findChild(EduNames.TEST);
          if (testDir == null) {
            testDir = parent.createChildDirectory(CCProjectComponent.class, EduNames.TEST);
          }
          testFile.move(CCProjectComponent.class, testDir);
        } catch (IOException e) {
          LOG.error(e);
        }
      });
    }
  }

  private static List<VirtualFile> getAllTestFiles(@NotNull Course course, @NotNull Project project) {
    List<VirtualFile> result = new ArrayList<>();
    for (Lesson lesson : course.getLessons()) {
      for (Task task : lesson.getTaskList()) {
        result.addAll(EduUtils.getTestFiles(task, project));
      }
    }
    return result;
  }

  @NotNull
  public String getComponentName() {
    return "CCProjectComponent";
  }

  public void projectOpened() {
    migrateIfNeeded();
    StartupManager.getInstance(myProject).runWhenProjectIsInitialized(() -> {
      if (CCUtils.isCourseCreator(myProject)) {
        registerListener();
        EduUsagesCollector.projectTypeOpened(CCUtils.COURSE_MODE);
      }
    });
  }

  public void registerListener() {
    if (myTaskFileLifeListener == null) {
      myTaskFileLifeListener = new CCVirtualFileListener(myProject);
      VirtualFileManager.getInstance().addVirtualFileListener(myTaskFileLifeListener);
    }
  }

  public void projectClosed() {
    if (myTaskFileLifeListener != null) {
      VirtualFileManager.getInstance().removeVirtualFileListener(myTaskFileLifeListener);
    }
  }
}
