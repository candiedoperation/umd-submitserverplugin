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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.apache.groovy.util.Arrays;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class SubmitLDAPCredDialog extends DialogWrapper {
    private JTextField usernameField;
    private JPasswordField passwordField;

    protected SubmitLDAPCredDialog(@Nullable Project project) {
        super(project, true);
        init();
        setTitle("Enter Your UMD Directory ID and Password");

        // Set the preferred size of the dialog
        setSize(550, getSize().height);
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Username label (placeholder)
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        JLabel usernameLabel = new JLabel("Directory ID (Ex. atheesh)");
        usernameLabel.setForeground(Color.GRAY);
        panel.add(usernameLabel, gbc);

        // Username field
        gbc.gridx = 0;
        gbc.gridy = 1;
        usernameField = new JTextField();
        panel.add(usernameField, gbc);

        // Password label (placeholder)
        gbc.gridx = 0;
        gbc.gridy = 2;
        JLabel passwordLabel = new JLabel("Password");
        passwordLabel.setForeground(Color.GRAY);
        panel.add(passwordLabel, gbc);

        // Password field
        gbc.gridx = 0;
        gbc.gridy = 3;
        passwordField = new JPasswordField();
        panel.add(passwordField, gbc);

        return panel;
    }

    // Getter methods to retrieve entered values
    public String getUsername() {
        return usernameField.getText();
    }

    public String getPassword() {
        return new String(passwordField.getPassword());
    }

    @Override
    public boolean isResizable() {
        return false;
    }

    @Override
    protected Action @NotNull [] createActions() {
        return new Action[]{getOKAction()};
    }
}
