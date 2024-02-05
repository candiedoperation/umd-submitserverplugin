/*
    University of Maryland CS Submit Server Plugin
    Copyright (C) 2024  Atheesh Thirumalairajan

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package org.atheesh.umdsubmitserverplugin;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class SubmitProjectAction extends AnAction {
    @Override
    public void update(AnActionEvent e) {
        /* Get the Current Project */
        Project currentProject = e.getProject();
        if (currentProject == null) e.getPresentation().setEnabled(false);
        else {
            /* Check if the .submit file Exists */
            boolean submitExists =
                Files.exists(Path.of(currentProject.getBasePath() + "/.submit"));

            /* Set Visibility Status */
            e.getPresentation().setEnabled(submitExists);
        }
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        ProgressManager.getInstance().run(
            new  Task.Modal(e.getProject(), "Submitting Project", false) {
                @Override
                public void run(@NotNull ProgressIndicator progressIndicator) {
                    /* Set Indeterminate Progress Indicator */
                    progressIndicator.setIndeterminate(true);

                    try {
                        /* Initiate Submission Middleware */
                        new SubmitServerMiddleware(e.getProject(), progressIndicator)
                                .initiateSubmission();
                    } catch (SubmitProjectException ex) {
                        /*
                            Since we're using the ProgressManager,
                            invokeLater is required for EDT thread
                        */

                        SwingUtilities.invokeLater(() -> {
                            /* Show Error Dialog */
                            Messages.showErrorDialog(
                                    ex.getMessage(),
                                    "Project Submission Error"
                            );
                        });
                    }
                }
            }
        );
    }
}
