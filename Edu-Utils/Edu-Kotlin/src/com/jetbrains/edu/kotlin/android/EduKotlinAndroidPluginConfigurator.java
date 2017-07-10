package com.jetbrains.edu.kotlin.android;

import com.intellij.ProjectTopics;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.ModuleListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.MessageBusConnection;
import com.jetbrains.edu.kotlin.EduKotlinPluginConfigurator;
import com.jetbrains.edu.learning.checker.StudyTaskChecker;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.courseFormat.tasks.PyCharmTask;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.courseGeneration.StudyGenerator;
import com.jetbrains.edu.learning.newproject.EduCourseProjectGenerator;
import com.jetbrains.edu.utils.EduIntellijUtils;
import com.jetbrains.edu.utils.EduPluginConfiguratorBase;
import org.jetbrains.android.sdk.AndroidSdkType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class EduKotlinAndroidPluginConfigurator extends EduKotlinPluginConfigurator {

  private static final String DEFAULT_COURSE_PATH = "AndroidCourse.zip";
  private static final Logger LOG = Logger.getInstance(EduPluginConfiguratorBase.class);
  private final EduKotlinAndroidCourseProjectGenerator myGenerator = new EduKotlinAndroidCourseProjectGenerator();

  @NotNull
  @Override
  public StudyTaskChecker<PyCharmTask> getPyCharmTaskChecker(@NotNull PyCharmTask pyCharmTask, @NotNull Project project) {
    return new StudyTaskChecker<>(pyCharmTask, project);
  }

  @Override
  public List<String> getBundledCoursePaths() {
    File bundledCourseRoot = EduIntellijUtils.getBundledCourseRoot(DEFAULT_COURSE_PATH, EduKotlinAndroidPluginConfigurator.class);
    return Collections.singletonList(FileUtil.join(bundledCourseRoot.getAbsolutePath(), DEFAULT_COURSE_PATH));
  }

  @Override
  public void createCourseModuleContent(@NotNull ModifiableModuleModel moduleModel,
                                        @NotNull Project project,
                                        @NotNull Course course,
                                        @Nullable String moduleDir) {
    final List<Lesson> lessons = course.getLessons();
    final Task task = lessons.get(0).getTaskList().get(0);

    if (moduleDir == null) {
      Logger.getInstance(EduPluginConfiguratorBase.class).error("Can't find module dir ");
      return;
    }

    final VirtualFile dir = VfsUtil.findFileByIoFile(new File(moduleDir), true);
    if (dir == null) {
      LOG.error("Can't find module dir on file system " + moduleDir);
      return;
    }

    ApplicationManager.getApplication().invokeLater(() -> ApplicationManager.getApplication().runWriteAction(() ->
      {
        try {
          StudyGenerator.createTaskContent(task, dir);
          MessageBusConnection connect = project.getMessageBus().connect();
          connect.subscribe(ProjectTopics.MODULES, new ModuleListener() {
            @Override
            public void moduleAdded(@NotNull Project project, @NotNull Module module) {
              ApplicationManager.getApplication().invokeLater(() -> ApplicationManager.getApplication().runWriteAction(() -> {
                List<Sdk> androidSdks = ProjectJdkTable.getInstance().getSdksOfType(AndroidSdkType.getInstance());
                if (!androidSdks.isEmpty()) {
                  Sdk androidSdk = androidSdks.get(0);
                  ModifiableRootModel modifiableModel = ModuleRootManager.getInstance(module).getModifiableModel();
                  modifiableModel.setSdk(androidSdk);
                  modifiableModel.commit();
                  module.getProject().save();
                }
              }));
            }
          });

        } catch (IOException e) {
          LOG.error(e);
        }
      }
    ));
  }

  @Override
  public EduCourseProjectGenerator getEduCourseProjectGenerator() {
    return myGenerator;
  }
}
