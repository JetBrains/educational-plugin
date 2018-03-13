package com.jetbrains.edu.coursecreator.actions.sections;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.jetbrains.edu.coursecreator.CCUtils;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.Section;

public class CCRenameSection extends DumbAwareAction {
  public static final String TITLE = "Rename Section";

  public CCRenameSection() {
    super(TITLE, TITLE, null);
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) {
      return;
    }
    final Object[] selectedItems = PlatformDataKeys.SELECTED_ITEMS.getData(e.getDataContext());
    final Course course = StudyTaskManager.getInstance(project).getCourse();
    if (course == null) {
      return;
    }
    if (selectedItems != null && selectedItems.length == 1 && selectedItems[0] instanceof Section) {
      final Section selectedSection = (Section)selectedItems[0];
      final String sectionName = Messages.showInputDialog("Enter Section Name", "Section", null);
      selectedSection.setTitle(sectionName);
    }

    ProjectView.getInstance(project).refresh();
  }

  @Override
  public void update(AnActionEvent e) {
    Project project = e.getProject();
    Presentation presentation = e.getPresentation();
    if (project != null && CCUtils.isCourseCreator(project)) {
      final Object[] selectedItems = PlatformDataKeys.SELECTED_ITEMS.getData(e.getDataContext());
      if (selectedItems != null && selectedItems.length == 1 && selectedItems[0] instanceof Section) {
        presentation.setEnabledAndVisible(true);
        return;
      }
    }
    presentation.setEnabledAndVisible(false);
  }
}