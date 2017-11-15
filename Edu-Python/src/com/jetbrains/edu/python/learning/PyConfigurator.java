package com.jetbrains.edu.python.learning;

import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.fileTemplates.FileTemplateUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import com.intellij.util.PlatformUtils;
import com.jetbrains.edu.learning.EduConfigurator;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.checker.TaskChecker;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.courseFormat.tasks.TaskWithSubtasks;
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils;
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator;
import com.jetbrains.edu.python.learning.checker.PyTaskChecker;
import com.jetbrains.edu.python.learning.newproject.PyDirectoryProjectGenerator;
import com.jetbrains.edu.python.learning.newproject.PyLanguageSettings;
import com.jetbrains.python.PythonModuleTypeBase;
import com.jetbrains.python.newProject.PyNewProjectSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class PyConfigurator implements EduConfigurator<PyNewProjectSettings> {
  public static final String PYTHON_3 = "3.x";
  public static final String PYTHON_2 = "2.x";
  private static final String TESTS_PY = "tests.py";
  private static final Logger LOG = Logger.getInstance(PyConfigurator.class);
  private static final String COURSE_NAME = "Introduction to Python.zip";
  private static final String TASK_PY = "task.py";

  @NotNull
  @Override
  public String getTestFileName() {
    return TESTS_PY;
  }

  @NotNull
  @Override
  public String getStepikDefaultLanguage() {
    return "python3";
  }

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
      } catch (IOException e) {
        LOG.error("Failed to create task directory", e);
      }
      if (taskDirectory.isNull()) return;

      if (EduUtils.isStudentProject(project) && !task.getTaskFiles().isEmpty()) {
        createFilesFromText(task, taskDirectory.get());
      } else {
        createFilesFromTemplates(project, task, taskDirectory.get());
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
  public boolean excludeFromArchive(@NotNull String path) {
    return path.contains("__pycache__") || path.endsWith(".pyc");
  }

  @Override
  public boolean isTestFile(VirtualFile file) {
    String name = file.getName();
    if (TESTS_PY.equals(name)) {
      return true;
    }
    return name.contains(FileUtil.getNameWithoutExtension(TESTS_PY)) && name.contains(EduNames.SUBTASK_MARKER);
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

  @Override
  @NotNull
  public TaskChecker<EduTask> getEduTaskChecker(@NotNull EduTask task, @NotNull Project project) {
    return new PyTaskChecker(task, project);
  }

  @Override
  public List<String> getBundledCoursePaths() {
    File bundledCourseRoot = EduUtils.getBundledCourseRoot(COURSE_NAME, PyConfigurator.class);
    return Collections.singletonList(FileUtil.join(bundledCourseRoot.getAbsolutePath(), COURSE_NAME));
  }

  @NotNull
  @Override
  public LanguageSettings<PyNewProjectSettings> getLanguageSettings() {
    return new PyLanguageSettings();
  }

  public ModuleType getModuleType() {
    return PythonModuleTypeBase.getInstance();
  }

  @Override
  public void configureModule(@NotNull Module module) {
    final Project project = module.getProject();
    StartupManager.getInstance(project).runWhenProjectIsInitialized(() -> {
      final VirtualFile baseDir = project.getBaseDir();
      final String testHelper = EduNames.TEST_HELPER;
      if (baseDir.findChild(testHelper) != null) return;
      final FileTemplate template = FileTemplateManager.getInstance(project).getInternalTemplate("test_helper");
      final PsiDirectory projectDir = PsiManager.getInstance(project).findDirectory(baseDir);
      if (projectDir == null) return;
      try {
        FileTemplateUtil.createFromTemplate(template, testHelper, null, projectDir);
      }
      catch (Exception exception) {
        LOG.error("Can't copy test_helper.py " + exception.getMessage());
      }
    });
  }

  @Override
  public boolean isEnabled() {
    return !(PlatformUtils.isPyCharm() || PlatformUtils.isCLion());
  }

  @Nullable
  @Override
  public CourseProjectGenerator<PyNewProjectSettings> getEduCourseProjectGenerator(@NotNull Course course) {
    return new PyDirectoryProjectGenerator(course);
  }
}
