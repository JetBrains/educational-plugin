package com.jetbrains.edu.learning.projectView;

import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.courseFormat.Section;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CourseNode extends EduNode<Course> {
  protected final ViewSettings myViewSettings;

  public CourseNode(@NotNull Project project,
                    PsiDirectory value,
                    ViewSettings viewSettings,
                    @NotNull Course course) {
    super(project, value, viewSettings, course);
    myViewSettings = viewSettings;
  }

  @Nullable
  @Override
  protected AbstractTreeNode modifyChildNode(@NotNull AbstractTreeNode child) {
    Object value = child.getValue();
    if (value instanceof PsiDirectory) {
      PsiDirectory directory = (PsiDirectory)value;
      final Section section = getItem().getSection(directory.getName());
      if (section != null) {
        return createSectionNode(directory, section);
      }
      final Lesson lesson = getItem().getLesson(directory.getName());
      if (lesson != null) {
        return createLessonNode(directory, lesson);
      }
    }
    return null;
  }

  @NotNull
  protected SectionNode createSectionNode(@NotNull PsiDirectory directory, @NotNull Section section) {
    return new SectionNode(myProject, getSettings(), section, directory);
  }

  @Nullable
  protected LessonNode createLessonNode(@NotNull PsiDirectory directory, @NotNull Lesson lesson) {
    if (lesson instanceof FrameworkLesson) {
      return FrameworkLessonNode.createFrameworkLessonNode(myProject, directory, getSettings(), (FrameworkLesson) lesson);
    } else {
      return new LessonNode(myProject, directory, getSettings(), lesson);
    }
  }

  @NotNull
  @Override
  public Course getItem() {
    Course item = super.getItem();
    assert item != null;
    return item;
  }
}
