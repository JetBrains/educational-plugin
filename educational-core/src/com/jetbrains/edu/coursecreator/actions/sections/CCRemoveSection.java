package com.jetbrains.edu.coursecreator.actions.sections;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.coursecreator.CCUtils;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.courseFormat.Section;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

public class CCRemoveSection extends DumbAwareAction {
  protected static final Logger LOG = Logger.getInstance(CCRemoveSection.class);
  public static final String TITLE = "Unwrap Section";

  public CCRemoveSection() {
    super(TITLE, TITLE, null);
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    Project project = e.getProject();
    final VirtualFile[] selectedFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
    if (project == null || selectedFiles == null || selectedFiles.length != 1) {
      return;
    }
    final Course course = StudyTaskManager.getInstance(project).getCourse();
    if (course == null) {
      return;
    }

    final VirtualFile file = selectedFiles[0];
    final Section section = course.getSection(file.getName());
    if (section == null) {
      return;
    }
    final VirtualFile[] sectionChildren = VfsUtil.getChildren(file);
    for (VirtualFile child : sectionChildren) {
      if (project.getBaseDir().findChild(child.getName()) != null) {
        Messages.showInfoMessage("Can't unwrap lesson " + child.getName() + ". Course contains directory " +
                                 "with the same name already.", "Unwrap Section Failed");
        return;
      }
    }
    final VirtualFile courseDir = EduUtils.getCourseDir(project);
    if (removeSectionDir(file, courseDir)) {
      final List<Lesson> lessonsFromSection = section.getLessons();
      final int sectionIndex = section.getIndex();

      for (Lesson lesson : lessonsFromSection) {
        lesson.setIndex(lesson.getIndex() + sectionIndex - 1);
        lesson.setSection(null);
      }
      course.addLessons(lessonsFromSection);
      course.sortItems();
    }

    ProjectView.getInstance(project).refresh();
  }

  private boolean removeSectionDir(@NotNull VirtualFile file, @NotNull VirtualFile courseDir) {
    return ApplicationManager.getApplication().runWriteAction(new Computable<Boolean>() {
      @Override
      public Boolean compute() {
        final VirtualFile[] children = VfsUtil.getChildren(file);
        for (VirtualFile child : children) {
          try {
            child.move(this, courseDir);
          }
          catch (IOException e) {
            LOG.error("Failed to move lesson " + child.getName() + " out of section");
            return false;
          }
        }
        try {
          file.delete(this);
        }
        catch (IOException e) {
          LOG.error("Failed to delete section " + file.getName());
          return false;
        }
        return true;
      }
    });
  }

  @Override
  public void update(AnActionEvent e) {
    Project project = e.getProject();
    Presentation presentation = e.getPresentation();
    presentation.setEnabledAndVisible(false);
    if (project == null) return;
    final Course course = StudyTaskManager.getInstance(project).getCourse();
    if (course == null) {
      return;
    }
    if (CCUtils.isCourseCreator(project)) {
      final VirtualFile[] selectedFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
      if (selectedFiles != null && selectedFiles.length == 1) {
        final Section section = course.getSection(selectedFiles[0].getName());
        if (section != null) {
          presentation.setEnabledAndVisible(true);
        }
      }
    }
  }
}