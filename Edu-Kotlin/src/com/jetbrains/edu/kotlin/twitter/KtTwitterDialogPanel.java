package com.jetbrains.edu.kotlin.twitter;

import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.util.ui.UIUtil;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.twitter.TwitterUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.InputStream;
import java.net.URL;

@SuppressWarnings("WeakerAccess")
public class KtTwitterDialogPanel extends TwitterUtils.TwitterDialogPanel {
    private final JTextArea myTwitterTextField;
    private final JLabel myRemainSymbolsLabel;
    private URL myImageUrl;
    private String imageName = "";

    public KtTwitterDialogPanel(@NotNull Task solvedTask) {
        setLayout(new VerticalFlowLayout());
        myRemainSymbolsLabel = new JLabel();
        myTwitterTextField = new JTextArea();
        myTwitterTextField.setLineWrap(true);
        create(solvedTask);
    }

    public void create(@NotNull Task solvedTask) {
        add(new JLabel(UIUtil.toHtml("<b>Post your achievements to twitter!<b>\n")));
        myImageUrl = getMediaSourceForTask(solvedTask);
        addImageLabel();

        String messageForTask = getMessageForTask(solvedTask);
        myTwitterTextField.setText(messageForTask);
        add(myTwitterTextField);

        myRemainSymbolsLabel.setText(String.valueOf(140 - messageForTask.length()));
        JPanel jPanel = new JPanel(new BorderLayout());
        jPanel.add(myRemainSymbolsLabel, BorderLayout.EAST);
        add(jPanel);
    }

    private void addImageLabel() {
        if (myImageUrl != null) {
            Icon icon = new ImageIcon(myImageUrl);
            add(new JLabel(icon));
        }
    }

    @NotNull
    @Override
    public String getMessage() {
        return myTwitterTextField.getText();
    }

    private static String getMessageForTask(@NotNull final Task task) {
        int solvedTaskNumber = KtTwitterConfigurator.calculateTaskNumber(task);
        return "Hey, I just completed level " + solvedTaskNumber / 8 
                +" of Kotlin Koans. https://kotlinlang.org/docs/tutorials/koans.html #kotlinkoans";
    }

    @Nullable
    @Override
    public InputStream getMediaSource() {
        return getClass().getResourceAsStream(imageName);
    }

    @Nullable
    @Override
    public String getMediaExtension() {
        return "gif";
    }

    @Nullable
    private URL getMediaSourceForTask(@NotNull final Task task) {
        imageName = "/twitter/kotlin_koans/images/" + getImageName(task);
        return getClass().getResource(imageName);
    }
    
    private static String getImageName(@NotNull final Task task) {
        int solvedTaskNumber = KtTwitterConfigurator.calculateTaskNumber(task);
        int level = solvedTaskNumber / 8;
        return level + "level.gif";
    }


    @Override
    public void addTextFieldVerifier(@NotNull DocumentListener documentListener) {
        myTwitterTextField.getDocument().addDocumentListener(documentListener);
    }

    @NotNull
    @Override
    public JLabel getRemainSymbolsLabel() {
        return myRemainSymbolsLabel;
    }
}
