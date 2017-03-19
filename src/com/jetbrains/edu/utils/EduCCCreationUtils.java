package com.jetbrains.edu.utils;

import com.intellij.ide.IdeView;
import com.intellij.ide.util.newProjectWizard.AbstractProjectWizard;
import com.intellij.ide.util.newProjectWizard.StepSequence;
import com.intellij.ide.util.projectWizard.ProjectBuilder;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.actions.NewModuleAction;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import com.jetbrains.edu.coursecreator.settings.CCSettings;
import com.jetbrains.edu.learning.StudyUtils;
import com.jetbrains.edu.learning.core.EduNames;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.courseFormat.StudyItem;
import com.jetbrains.edu.learning.courseFormat.Task;
import com.jetbrains.edu.utils.generation.EduLessonModuleBuilder;
import com.jetbrains.edu.utils.generation.EduTaskModuleBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class EduCCCreationUtils {
  private EduCCCreationUtils() {
  }

  public static PsiDirectory createLesson(@NotNull Project project, @NotNull StudyItem item, @NotNull PsiDirectory parentDirectory) {
    NewModuleAction newModuleAction = new NewModuleAction();
    String courseDirPath = parentDirectory.getVirtualFile().getPath();
    Lesson lesson = (Lesson) item;
    Module utilModule = ModuleManager.getInstance(project).findModuleByName(EduIntelliJNames.UTIL);
    if (utilModule == null) {
      return null;
    }
    newModuleAction.createModuleFromWizard(project, null, new AbstractProjectWizard("", project, "") {
      @Override
      public StepSequence getSequence() {
        return null;
      }

      @Override
      public ProjectBuilder getProjectBuilder() {
        return new EduLessonModuleBuilder(courseDirPath, lesson, utilModule);
      }
    });
    return parentDirectory.findSubdirectory(EduNames.LESSON + lesson.getIndex());
  }

  public static PsiDirectory createTask(@NotNull Project project, @NotNull StudyItem item, @Nullable IdeView view, @NotNull PsiDirectory parentDirectory,
                                        @Nullable String taskFileName, @Nullable String testFileName) {
    String lessonDirName = parentDirectory.getName();
    NewModuleAction newModuleAction = new NewModuleAction();
    Module lessonModule = ModuleManager.getInstance(project).findModuleByName(lessonDirName);
    Module utilModule = ModuleManager.getInstance(project).findModuleByName(EduIntelliJNames.UTIL);
    if (lessonModule == null || utilModule == null) {
      return null;
    }
    Task task = (Task) item;
    newModuleAction.createModuleFromWizard(project, null, new AbstractProjectWizard("", project, "") {
      @Override
      public StepSequence getSequence() {
        return null;
      }

      @Override
      public ProjectBuilder getProjectBuilder() {
        return new EduTaskModuleBuilder(parentDirectory.getVirtualFile().getPath(), lessonDirName, task, utilModule) {
          @Override
          protected void createTask(Project project, Course course, VirtualFile src) throws IOException {
            PsiDirectory psiDirectory = PsiManager.getInstance(project).findDirectory(src);
            if (psiDirectory == null) {
              return;
            }
            String taskDescriptionFileName = StudyUtils.getTaskDescriptionFileName(CCSettings.getInstance().useHtmlAsDefaultTaskFormat());
            StudyUtils.createFromTemplate(project, psiDirectory, taskDescriptionFileName, view, false);
            createIfNotNull(project, psiDirectory, taskFileName, view);
            createIfNotNull(project, psiDirectory, testFileName, view);
          }
        };
      }
    });
    return parentDirectory.findSubdirectory(EduNames.LESSON + task.getLesson().getIndex() + "-" + EduNames.TASK + task.getIndex());
  }

  private static void createIfNotNull(@NotNull Project project, @NotNull PsiDirectory psiDirectory, @Nullable String fileName, @Nullable IdeView view) {
    if (fileName != null) {
      StudyUtils.createFromTemplate(project, psiDirectory, fileName, view, false);
    }
  }

}
