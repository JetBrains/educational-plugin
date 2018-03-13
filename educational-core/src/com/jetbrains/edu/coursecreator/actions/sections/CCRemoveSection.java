package com.jetbrains.edu.coursecreator.actions.sections;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.jetbrains.edu.coursecreator.CCUtils;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.Section;

import java.util.List;

public class CCRemoveSection extends DumbAwareAction {
  public static final String TITLE = "Unwrap Section";

  public CCRemoveSection() {
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
      final List<Section> sections = course.getSections();
      sections.removeIf(section1 -> section1.equals(selectedSection));
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