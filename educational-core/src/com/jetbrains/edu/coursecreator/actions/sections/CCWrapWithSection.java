package com.jetbrains.edu.coursecreator.actions.sections;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.coursecreator.CCUtils;
import com.jetbrains.edu.coursecreator.stepik.StepikCourseChangeHandler;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class CCWrapWithSection extends DumbAwareAction {
  protected static final Logger LOG = Logger.getInstance(CCWrapWithSection.class);

  public static final String TITLE = "Wrap With Section";
  @NonNls private static final String SECTION = "Section";

  public CCWrapWithSection() {
    super(TITLE, TITLE, null);
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    Project project = e.getProject();
    final VirtualFile[] virtualFiles = CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(e.getDataContext());
    if (project == null || virtualFiles == null) {
      return;
    }
    final Course course = StudyTaskManager.getInstance(project).getCourse();
    if (course == null) {
      return;
    }

    final ArrayList<Lesson> lessonsToWrap = getLessonsToWrap(virtualFiles, course);
    if (lessonsToWrap.isEmpty()) {
      return;
    }

    final int sectionIndex = course.getSections().size() + 1;
    final String sectionName = Messages.showInputDialog("Enter Section Name", SECTION, null,
                                                        SECTION.toLowerCase() + sectionIndex,
                                                        new CCUtils.PathInputValidator(EduUtils.getCourseDir(project)));
    if (sectionName == null) {
      return;
    }

    if (!CCUtils.wrapIntoSection(project, course, lessonsToWrap, sectionName)) return;
    ProjectView.getInstance(project).refresh();
    StepikCourseChangeHandler.INSTANCE.contentChanged(course);
  }

  @NotNull
  private static ArrayList<Lesson> getLessonsToWrap(@NotNull VirtualFile[] virtualFiles, @NotNull Course course) {
    final ArrayList<Lesson> lessonsToWrap = new ArrayList<>();
    for (VirtualFile file : virtualFiles) {
      final Lesson lesson = course.getLesson(file.getName());
      if (lesson != null) {
        lessonsToWrap.add(lesson);
      }
    }
    return lessonsToWrap;
  }

  @Override
  public void update(AnActionEvent e) {
    Project project = e.getProject();
    Presentation presentation = e.getPresentation();
    presentation.setEnabledAndVisible(false);
    if (project == null || !CCUtils.isCourseCreator(project)) {
      return;
    }
    final VirtualFile[] virtualFiles = CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(e.getDataContext());
    if (virtualFiles == null || virtualFiles.length == 0) {
      return;
    }
    final Course course = StudyTaskManager.getInstance(project).getCourse();
    if (course == null) {
      return;
    }
    final ArrayList<Lesson> lessonsToWrap = getLessonsToWrap(virtualFiles, course);
    if (!lessonsToWrap.isEmpty()) {
      presentation.setEnabledAndVisible(true);
    }
  }
}