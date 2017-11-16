package com.jetbrains.edu.learning.intellij;

import com.intellij.lang.Language;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils;
import com.jetbrains.edu.learning.intellij.generation.EduModuleBuilderUtils;
import com.jetbrains.edu.learning.intellij.generation.TaskModuleBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

import static com.jetbrains.edu.learning.EduUtils.createFromTemplate;

/**
 * Contains code shared between Java and Kotlin modules
 */
public class EduIntellijUtils {
    private static final Logger LOG = Logger.getInstance(EduIntellijUtils.class);

    private EduIntellijUtils() {
    }


  public static VirtualFile createTask(@NotNull Project project, @NotNull Task task, @NotNull VirtualFile parentDirectory,
                                        @Nullable String taskFileName, @Nullable String testFileName) {
    String lessonDirName = parentDirectory.getName();
    Module lessonModule = ModuleManager.getInstance(project).findModuleByName(lessonDirName);
    Module utilModule = ModuleManager.getInstance(project).findModuleByName(EduNames.UTIL);
    if (lessonModule == null || utilModule == null) {
      return null;
    }
    TaskModuleBuilder moduleBuilder = new TaskModuleBuilder(parentDirectory.getPath(), lessonDirName, task, utilModule) {
      @Override
      protected void createTask(Project project, Course course, VirtualFile src) throws IOException {
        if (taskFileName == null) {
          return;
        }

        if (course.isAdaptive()) {
          createFromText(project, taskFileName, task);
        } else {
          createFromTemplate(project, src, taskFileName);
          task.addTaskFile(taskFileName, task.taskFiles.size());
          if (testFileName != null) {
            createFromTemplate(project, src, testFileName);
          }
        }
      }
    };
    EduModuleBuilderUtils.createModule(project, moduleBuilder, "");
    return parentDirectory.findChild(EduNames.LESSON + task.getLesson().getIndex() + "-" + EduNames.TASK + task.getIndex());
  }

  private static void createFromText(@NotNull Project project, @Nullable String taskFileName, @NotNull Task task) {
    TaskFile taskFile = task.getTaskFile(taskFileName);
    VirtualFile taskDir = task.getTaskDir(project);
    if (taskFile != null && taskDir != null) {
      taskFile.text = StringUtil.notNullize(taskFile.text);
      nameTaskFileAfterContainingClass(task, taskFile, project);

      try {
        GeneratorUtils.createTaskFile(taskDir, taskFile);
      }
      catch (IOException e) {
        LOG.warn(e.getMessage());
      }
    }
  }

  public static void nameTaskFileAfterContainingClass(@NotNull Task task,
                                                      @NotNull TaskFile taskFile,
                                                      @NotNull Project project) {
    Language language = task.getLesson().getCourse().getLanguageById();
    if (language.getAssociatedFileType() == null) {
      LOG.warn("Cannot rename task file. Unable to find associated file type for language: " + language.getID());
      return;
    }
    task.getTaskFiles().remove(taskFile.name);
    taskFile.name = publicClassName(project, taskFile, language.getAssociatedFileType()) + "." + language.getAssociatedFileType().getDefaultExtension();
    task.taskFiles.put(taskFile.name, taskFile);
  }

  @NotNull
  private static String publicClassName(@NotNull Project project, @NotNull TaskFile taskFile, @NotNull LanguageFileType fileType) {
    String fileName = "Main";
    PsiFile file = PsiFileFactory.getInstance(project).createFileFromText(taskFile.name, fileType, taskFile.text);
    if (file instanceof PsiClassOwner) {
      PsiClassOwner fileFromText = (PsiClassOwner) file;
      PsiClass[] classes = fileFromText.getClasses();
      for (PsiClass aClass : classes) {
        boolean isPublic = aClass.hasModifierProperty(PsiModifier.PUBLIC);
        if (isPublic && aClass.getName() != null) {
          fileName = aClass.getName();
          break;
        }
      }
    }

    return fileName;
  }

}
