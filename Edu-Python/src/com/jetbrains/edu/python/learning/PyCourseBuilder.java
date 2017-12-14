package com.jetbrains.edu.python.learning;

import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.fileTemplates.FileTemplateUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import com.jetbrains.edu.coursecreator.CCUtils;
import com.jetbrains.edu.learning.EduCourseBuilder;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.courseFormat.tasks.TaskWithSubtasks;
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils;
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator;
import com.jetbrains.edu.python.learning.newproject.PyDirectoryProjectGenerator;
import com.jetbrains.edu.python.learning.newproject.PyLanguageSettings;
import com.jetbrains.python.newProject.PyNewProjectSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

import static com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.createDescriptionFiles;
import static com.jetbrains.edu.python.learning.PyConfigurator.TASK_PY;
import static com.jetbrains.edu.python.learning.PyConfigurator.TESTS_PY;

public class PyCourseBuilder implements EduCourseBuilder<PyNewProjectSettings> {

  private static final Logger LOG = Logger.getInstance(PyCourseBuilder.class);

  @Override
  public VirtualFile createTaskContent(@NotNull Project project,
                                       @NotNull Task task,
                                       @NotNull VirtualFile parentDirectory,
                                       @NotNull Course course) {
    final Ref<VirtualFile> taskDirectory = new Ref<>();
    ApplicationManager.getApplication().runWriteAction(() -> {
      String taskDirName = EduNames.TASK + task.getIndex();
      try {
        taskDirectory.set(VfsUtil.createDirectoryIfMissing(parentDirectory, taskDirName));

        if (taskDirectory.isNull()) return;

        if (EduUtils.isStudentProject(project) && !task.getTaskFiles().isEmpty()) {
          createFilesFromText(task, taskDirectory.get());
        } else {
          createFilesFromTemplates(project, task, taskDirectory.get());
        }
        if (CCUtils.COURSE_MODE == task.getLesson().getCourse().getCourseMode()) {
          createDescriptionFiles(taskDirectory.get(), task);
        }
      } catch (IOException e) {
        LOG.error("Failed to create task content", e);
      }
    });
    return taskDirectory.get();
  }

  private static void createFilesFromText(@NotNull Task task, @Nullable VirtualFile taskDirectory) {
    if (taskDirectory == null) {
      LOG.warn("Task directory is null. Cannot create task files");
      return;
    }

    try {
      for (TaskFile file : task.getTaskFiles().values()) {
        GeneratorUtils.createTaskFile(taskDirectory, file);
      }
      GeneratorUtils.createTestFiles(taskDirectory, task);
    }
    catch (IOException e) {
      LOG.warn(e.getMessage());
    }
  }


  private static void createFilesFromTemplates(@NotNull Project project,
                                               @NotNull Task task,
                                               @NotNull VirtualFile taskDirectory) {
    EduUtils.createFromTemplate(project, taskDirectory, TASK_PY);
    EduUtils.createFromTemplate(project, taskDirectory, TESTS_PY);
    task.addTaskFile(TASK_PY, task.taskFiles.size());
  }

  @Override
  public void createTestsForNewSubtask(@NotNull Project project, @NotNull TaskWithSubtasks task) {
    VirtualFile taskDir = task.getTaskDir(project);
    if (taskDir == null) {
      return;
    }
    int nextSubtaskIndex = task.getLastSubtaskIndex() + 1;
    String nextSubtaskTestsFileName = getSubtaskTestsFileName(nextSubtaskIndex);
    ApplicationManager.getApplication().runWriteAction(() -> {
      try {
        PsiDirectory taskPsiDir = PsiManager.getInstance(project).findDirectory(taskDir);
        FileTemplate testsTemplate = FileTemplateManager.getInstance(project).getInternalTemplate(TESTS_PY);
        if (taskPsiDir == null || testsTemplate == null) {
          return;
        }
        FileTemplateUtil.createFromTemplate(testsTemplate, nextSubtaskTestsFileName, null, taskPsiDir);
      }
      catch (Exception e) {
        LOG.error(e);
      }
    });
  }

  @NotNull
  public static String getSubtaskTestsFileName(int index) {
    return index == 0 ? TESTS_PY : FileUtil.getNameWithoutExtension(TESTS_PY) +
            EduNames.SUBTASK_MARKER +
            index + "." +
            FileUtilRt.getExtension(TESTS_PY);
  }

  @NotNull
  @Override
  public LanguageSettings<PyNewProjectSettings> getLanguageSettings() {
    return new PyLanguageSettings();
  }

  @Nullable
  @Override
  public CourseProjectGenerator<PyNewProjectSettings> getCourseProjectGenerator(@NotNull Course course) {
    return new PyDirectoryProjectGenerator(course);
  }
}
