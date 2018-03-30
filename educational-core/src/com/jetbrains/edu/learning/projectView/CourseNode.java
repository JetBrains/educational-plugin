package com.jetbrains.edu.learning.projectView;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.ui.SimpleTextAttributes;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.courseFormat.Section;
import icons.EducationalCoreIcons;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;


public class CourseNode extends EduNode {
  protected final Course myCourse;

  public CourseNode(@NotNull Project project,
                    PsiDirectory value,
                    ViewSettings viewSettings,
                    @NotNull Course course) {
    super(project, value, viewSettings);
    myCourse = course;
  }

  @Override
  protected void updateImpl(PresentationData data) {
    Pair<Integer, Integer> progress = ProgressUtil.countProgressAsOneTaskWithSubtasks(myCourse.getLessons());
    if (progress == null) {
      progress = ProgressUtil.countProgressWithoutSubtasks(myCourse.getLessons());
    }

    final Integer tasksSolved = progress.getFirst();
    final Integer tasksTotal = progress.getSecond();
    data.clearText();
    data.addText(myCourse.getName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
    data.setIcon(EducationalCoreIcons.Course);
    data.addText("  " + tasksSolved.toString() + "/" + tasksTotal.toString(), SimpleTextAttributes.GRAYED_ATTRIBUTES);
  }

  protected AbstractTreeNode modifyChildNode(AbstractTreeNode child) {
    Object value = child.getValue();
    if (value instanceof PsiDirectory) {
      PsiDirectory directory = (PsiDirectory)value;
      final Section section = myCourse.getSection(directory.getName());
      if (section != null) {
        return createSectionNode(directory, section);
      }
      final Lesson lesson = myCourse.getLesson(directory.getName());
      if (lesson != null) {
        return createLessonNode(directory, lesson);
      }
    }
    return null;
  }

  @NotNull
  protected SectionNode createSectionNode(PsiDirectory directory, Section section) {
    return new SectionNode(myProject, getSettings(), section, directory);
  }

  @NotNull
  protected LessonNode createLessonNode(PsiDirectory directory, Lesson lesson) {
    if (lesson instanceof FrameworkLesson) {
      return new FrameworkLessonNode(myProject, directory, getSettings(), (FrameworkLesson) lesson);
    } else {
      return new LessonNode(myProject, directory, getSettings(), lesson);
    }
  }
}
