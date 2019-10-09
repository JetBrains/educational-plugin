package com.jetbrains.edu.coursecreator.handlers.move;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.move.MoveCallback;
import com.jetbrains.edu.coursecreator.CCUtils;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.OpenApiExtKt;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.*;
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class CCLessonMoveHandlerDelegate extends CCStudyItemMoveHandlerDelegate {

  private static final Logger LOG = Logger.getInstance(CCLessonMoveHandlerDelegate.class);

  public CCLessonMoveHandlerDelegate() {
    super(EduNames.LESSON);
  }

  @Override
  protected boolean isAvailable(@NotNull PsiDirectory directory) {
    return EduUtils.isLessonDirectory(directory.getProject(), directory.getVirtualFile());
  }

  @Override
  public void doMove(final Project project,
                     PsiElement[] elements,
                     @Nullable PsiElement targetDirectory,
                     @Nullable MoveCallback callback) {
    if (!(targetDirectory instanceof PsiDirectory)) {
      return;
    }
    final Course course = StudyTaskManager.getInstance(project).getCourse();
    if (course == null) {
      return;
    }
    final PsiDirectory sourceDirectory = (PsiDirectory)elements[0];
    final VirtualFile sourceVFile = sourceDirectory.getVirtualFile();
    final Lesson sourceLesson = EduUtils.getLesson(project, course, sourceVFile);
    if (sourceLesson == null) {
      throw new IllegalStateException("Failed to find lesson for `sourceVFile` directory");
    }

    final VirtualFile targetVFile = ((PsiDirectory)targetDirectory).getVirtualFile();
    StudyItem targetItem = getTargetItem(course, targetVFile, project);
    if (targetItem == null) {
      Messages.showInfoMessage("Lessons can be moved only to other lessons or sections", "Incorrect Target For Move");
      return;
    }
    VirtualFile sourceParentDir = sourceVFile.getParent();
    VirtualFile targetParentDir = targetItem instanceof Lesson ? targetVFile.getParent() : targetVFile;

    if (targetItem instanceof Section || targetItem instanceof Course) {
      if (targetParentDir.findChild(sourceLesson.getName()) != null) {
        String prefix = targetItem instanceof Section ? "Section" : "Course";
        Messages.showInfoMessage(prefix + " contains lesson with the same name", "Incorrect Target For Move");
        return;
      }

    }
    final Section targetSection = course.getSection(targetParentDir.getName());
    final LessonContainer targetContainer = targetSection != null ? targetSection : course;

    int delta = 0;
    if (targetItem instanceof Lesson) {
      delta = getDelta(project, targetItem);
    }
    if (delta == -1) {
      return;
    }

    final LessonContainer sourceContainer = sourceLesson.getContainer();

    int sourceLessonIndex = sourceLesson.getIndex();
    sourceLesson.setIndex(-1);
    CCUtils.updateHigherElements(sourceParentDir.getChildren(), file -> sourceContainer.getItem(file.getName()), sourceLessonIndex, -1);

    final int newItemIndex = targetItem instanceof Lesson ? targetItem.getIndex() + delta : ((ItemContainer)targetItem).getItems().size() + 1;
    CCUtils.updateHigherElements(targetParentDir.getChildren(), file -> targetContainer.getItem(file.getName()), newItemIndex - 1, 1);

    sourceLesson.setIndex(newItemIndex);
    sourceLesson.setSection(targetSection);

    sourceContainer.removeLesson(sourceLesson);
    targetContainer.addLesson(sourceLesson);

    course.sortItems();
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        try {
          if (!targetParentDir.equals(sourceVFile.getParent())) {
            sourceVFile.move(this, targetParentDir);
          }
        }
        catch (IOException e) {
          LOG.error(e);
        }
      }
    });
    ProjectView.getInstance(project).refresh();
    YamlFormatSynchronizer.saveItem(targetContainer);
    YamlFormatSynchronizer.saveItem(sourceContainer);
  }

  private static StudyItem getTargetItem(@NotNull Course course, @NotNull VirtualFile targetVFile, @NotNull Project project) {
    if (targetVFile.equals(OpenApiExtKt.getCourseDir(project))) return course;
    StudyItem targetItem = course.getItem(targetVFile.getName());
    if (targetItem == null) {
      targetItem = EduUtils.getLesson(project, course, targetVFile);
    }
    return targetItem;
  }
}
