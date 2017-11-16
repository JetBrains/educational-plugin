package com.jetbrains.edu.kotlin;

import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.fileTemplates.FileTemplateUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.SubtaskUtils;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.courseFormat.tasks.TaskWithSubtasks;
import com.jetbrains.edu.learning.intellij.EduCourseBuilderBase;
import com.jetbrains.edu.learning.intellij.EduIntellijUtils;
import com.jetbrains.edu.learning.intellij.JdkProjectSettings;
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator;
import org.jetbrains.annotations.NotNull;

import java.util.Properties;

public class KtCourseBuilder extends EduCourseBuilderBase {

  public static final String TESTS_KT = "Tests.kt";
  private static final String SUBTASK_TESTS_KT = "Subtask_Tests.kt";
  public static final String TASK_KT = "Task.kt";

  @Override
  public void configureModule(@NotNull Module module) {
    super.configureModule(module);
    Project project = module.getProject();
    KtLibConfigurator.configureLib(project);
  }

  @Override
  public VirtualFile createTaskContent(@NotNull Project project, @NotNull Task task,
                                       @NotNull VirtualFile parentDirectory, @NotNull Course course) {
    return EduIntellijUtils.createTask(project, task, parentDirectory, TASK_KT, TESTS_KT);
  }

  @Override
  public void createTestsForNewSubtask(@NotNull Project project, @NotNull TaskWithSubtasks task) {
    VirtualFile taskDir = task.getTaskDir(project);
    if (taskDir == null) {
      return;
    }
    int prevSubtaskIndex = task.getLastSubtaskIndex();
    PsiDirectory taskPsiDir = PsiManager.getInstance(project).findDirectory(taskDir);
    if (taskPsiDir == null) {
      return;
    }
    int nextSubtaskIndex = prevSubtaskIndex + 1;
    String nextSubtaskFileName = SubtaskUtils.getTestFileName(project, nextSubtaskIndex);

    ApplicationManager.getApplication().runWriteAction(() -> {
      try {
        FileTemplate testsTemplate = FileTemplateManager.getInstance(project).getInternalTemplate(SUBTASK_TESTS_KT);
        if (testsTemplate == null) {
          return;
        }
        Properties properties = new Properties();
        properties.setProperty("TEST_CLASS_NAME", "Test" + EduNames.SUBTASK_MARKER + nextSubtaskIndex);
        FileTemplateUtil.createFromTemplate(testsTemplate, nextSubtaskFileName, properties, taskPsiDir);
      }
      catch (Exception e) {
        LOG.error(e);
      }
    });
  }

  public CourseProjectGenerator<JdkProjectSettings> getEduCourseProjectGenerator(@NotNull Course course) {
    return new KtCourseProjectGenerator(course);
  }
}
