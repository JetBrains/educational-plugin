package com.jetbrains.edu.coursecreator.projectView;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.nodes.PsiFileNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.ui.SimpleTextAttributes;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.projectView.CourseViewUtils;

/**
 * represents a file which is invisible for student in student mode
 */
public class CCStudentInvisibleFileNode extends PsiFileNode {
  private final String myName;

  public CCStudentInvisibleFileNode(Project project,
                                    PsiFile value,
                                    ViewSettings viewSettings) {
    this(project, value, viewSettings, value.getName());
  }

  public CCStudentInvisibleFileNode(Project project,
                                    PsiFile value,
                                    ViewSettings viewSettings,
                                    String name) {
    super(project, value, viewSettings);
    VirtualFile file = value.getVirtualFile();
    boolean isExcluded = file != null && EduUtils.canBeAddedToTask(project, file);
    // TODO: come up with better way to show user that this file doesn't belong to task
    myName = isExcluded ? name + " (excluded)" : name;
  }

  @Override
  protected void updateImpl(PresentationData data) {
    super.updateImpl(data);
    data.clearText();
    data.addText(myName, SimpleTextAttributes.GRAY_ATTRIBUTES);
  }

  @SuppressWarnings("deprecation")
  @Override
  public String getTestPresentation() {
    return CourseViewUtils.testPresentation(this);
  }
}
