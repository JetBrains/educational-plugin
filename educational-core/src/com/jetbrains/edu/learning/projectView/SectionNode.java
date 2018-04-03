package com.jetbrains.edu.learning.projectView;

import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleTextAttributes;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;


public class SectionNode extends EduNode {
  private final Section mySection;

  public SectionNode(@NotNull Project project, @NotNull ViewSettings viewSettings, @NotNull Section section, @Nullable PsiDirectory psiDirectory) {
    super(project, psiDirectory, viewSettings);
    mySection = section;
  }

  @Override
  public void update(PresentationData data) {
    boolean allSolved = isSolved();
    JBColor color = allSolved ? LIGHT_GREEN : JBColor.BLACK;
    Icon icon = allSolved ? AllIcons.Nodes.Package : AllIcons.Nodes.Folder; //TODO: use proper icons
    final SimpleTextAttributes textAttributes = new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, color);
    data.addText(mySection.getPresentableName(), textAttributes);
    data.setIcon(icon);
  }

  private boolean isSolved() {
    final Course course = StudyTaskManager.getInstance(myProject).getCourse();
    if (course != null) {
      return mySection.getLessons().stream().noneMatch(it -> it.getStatus() != CheckStatus.Solved);
    }
    return true;
  }

  @Override
  protected AbstractTreeNode modifyChildNode(AbstractTreeNode child) {
    Object value = child.getValue();
    if (value instanceof PsiDirectory) {
      PsiDirectory directory = (PsiDirectory)value;
      final Lesson lesson = mySection.getLesson(directory.getName());
      if (lesson != null) {
        return createLessonNode(directory, lesson);
      }
    }
    return null;
  }

  @NotNull
  protected LessonNode createLessonNode(@NotNull PsiDirectory directory, @NotNull Lesson lesson) {
    if (lesson instanceof FrameworkLesson) {
      return new FrameworkLessonNode(myProject, directory, getSettings(), (FrameworkLesson) lesson);
    } else {
      return new LessonNode(myProject, directory, getSettings(), lesson);
    }
  }

  @Override
  public int getWeight() {
    return mySection.getIndex();
  }
}
