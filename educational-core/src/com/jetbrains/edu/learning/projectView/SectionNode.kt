package com.jetbrains.edu.learning.projectView;

import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.courseFormat.Section;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class SectionNode extends EduNode<Section> {
  @NotNull protected final Project myProject;
  @NotNull protected final ViewSettings myViewSettings;

  public SectionNode(@NotNull Project project, @NotNull ViewSettings viewSettings, @NotNull Section section, @Nullable PsiDirectory psiDirectory) {
    super(project, psiDirectory, viewSettings, section);
    myProject = project;
    myViewSettings = viewSettings;
  }

  @Nullable
  @Override
  protected AbstractTreeNode modifyChildNode(@NotNull AbstractTreeNode child) {
    Object value = child.getValue();
    if (value instanceof PsiDirectory) {
      PsiDirectory directory = (PsiDirectory)value;
      final Lesson lesson = getItem().getLesson(directory.getName());
      if (lesson != null) {
        return createLessonNode(directory, lesson);
      }
    }
    return null;
  }

  @Nullable
  protected LessonNode createLessonNode(@NotNull PsiDirectory directory, @NotNull Lesson lesson) {
    if (lesson instanceof FrameworkLesson) {
      return FrameworkLessonNode.createFrameworkLessonNode(myProject, directory, getSettings(), (FrameworkLesson) lesson);
    } else {
      return new LessonNode(myProject, directory, getSettings(), lesson);
    }
  }

  @Override
  public int getWeight() {
    return getItem().getIndex();
  }

  @NotNull
  @Override
  public Section getItem() {
    Section item = super.getItem();
    assert item != null;
    return item;
  }
}
