package com.jetbrains.edu.learning.projectView;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.nodes.ProjectViewDirectoryHelper;
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleTextAttributes;
import com.jetbrains.edu.learning.courseFormat.StudyItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;

public abstract class EduNode<T extends StudyItem> extends PsiDirectoryNode {
  @Nullable private final T myItem;

  public EduNode(@NotNull final Project project,
                 PsiDirectory value,
                 ViewSettings viewSettings,
                 @Nullable T item) {
    super(project, value, viewSettings);
    myItem = item;
    myName = value.getName();
  }

  @Override
  protected void updateImpl(@NotNull PresentationData data) {
    data.clearText();
    if (myItem != null) {
      String name = myItem.getPresentableName();
      Icon icon = CourseViewUtils.getIcon(myItem);
      data.addText(name, new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor.BLACK));
      String additionalInfo = CourseViewUtils.getAdditionalInformation(myItem);
      if (additionalInfo != null) {
        data.addText(" " + additionalInfo, SimpleTextAttributes.GRAYED_ATTRIBUTES);
      }
      data.setIcon(icon);
    }
  }

  @Nullable
  protected T getItem() {
    return myItem;
  }

  @Override
  protected boolean hasProblemFileBeneath() {
    return false;
  }

  @Override
  public String getTestPresentation() {
    return CourseViewUtils.testPresentation(this);
  }

  @Override
  public Collection<AbstractTreeNode> getChildrenImpl() {
    final ArrayList<AbstractTreeNode> result = new ArrayList<>();
    final Collection<AbstractTreeNode> children =
      ProjectViewDirectoryHelper.getInstance(myProject).getDirectoryChildren(getValue(), getSettings(),
                                                                             true, null);
    for (AbstractTreeNode child : children) {
      final AbstractTreeNode node = modifyChildNode(child);
      if (node != null) {
        result.add(node);
      }
    }
    return result;
  }

  @Nullable
  protected AbstractTreeNode modifyChildNode(@NotNull AbstractTreeNode child) {
    return child;
  }
}
