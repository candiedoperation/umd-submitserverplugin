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

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.CompressionMethod;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class SubmitServerMiddleware {
    private Project project;
    private HashMap<String, String> config;
    private ProgressIndicator progressIndicator;
    private CloseableHttpClient httpClient;

    public SubmitServerMiddleware(Project project, ProgressIndicator progressIndicator) throws SubmitProjectException {
        /* Initialize Instance Variables */
        this.project = project;
        this.config = new HashMap<>();
        this.progressIndicator = progressIndicator;
        this.httpClient = HttpClients.createDefault();

        /* Call Init Methods */
        parseSubmitServerConfig();
    }

    public void initiateSubmission() throws SubmitProjectException {
        /* Update Progress */
        this.progressIndicator.setText("Authenticating with the submit server");

        /* Check Authentication Method in Config */
        if (this.config.get("authentication.type").equals("ldap"))
            negotiateCredentialsLDAP();
        else
            throw new SubmitProjectException("Authentication Type not Supported");

        /* Update Progress */
        this.progressIndicator.setText("Archiving and uploading files to the submit server");

        /* Auth Successful, Archive Project Files */
        try {
            /* Define Archive File */
            File dst = new File(this.project.getBasePath() + "/submit.zip");
            ZipFile archiveFile = new ZipFile(dst);

            /* Disable Compression */
            ZipParameters zipParameters = new ZipParameters();
            zipParameters.setCompressionMethod(CompressionMethod.STORE);

            /* Add folder to Archive */
            archiveFile.addFolder(
                new File(this.project.getBasePath() + "/src"),
                zipParameters
            );

            /* Add Comment to Archive */
            archiveFile.setComment("ZipFile for submission: candiedoperation/umd-submitserverplugin");

            /* We're successful with archiving, Upload the file */
            HttpPost request = new HttpPost(this.config.get("submitURL"));
            MultipartEntityBuilder mpeBuilder = MultipartEntityBuilder.create();

            /* Add the ZIP file as a binary entity */
            mpeBuilder.addBinaryBody(
                    "submittedFiles",
                    dst,
                    ContentType.APPLICATION_OCTET_STREAM,
                    "submit.zip"
            );

            /* Add Config Parameters */
            for (String configKey : this.config.keySet()) {
                String configVal = this.config.get(configKey);
                if (!configVal.equals("submitURL") && !configVal.equals("password"))
                    mpeBuilder.addTextBody(configKey, configVal);
            }

            /* Build and Execute Query */
            request.setEntity(mpeBuilder.build());
            CloseableHttpResponse response = httpClient.execute(request);
            String responseString = EntityUtils.toString(response.getEntity());

            /* Set a short Delay */
            Thread.sleep(250);

            /* Parse Response String */
            if (!responseString.startsWith("Successful"))
                throw new SubmitProjectException("Project Upload Failed: " + responseString);

            /* Show Successful Upload Dialog */
            SwingUtilities.invokeLater(() -> {
                Messages.showMessageDialog(
                        this.project,
                        responseString,
                        "Project Successfully Submitted",
                        Messages.getInformationIcon()
                );
            });
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            throw new SubmitProjectException("Failed to Archive project files");
        } finally {
            try {
                Files.deleteIfExists(Path.of(this.project.getBasePath() + "/submit.zip"));
            } catch (Exception ex) {
                System.err.println("Failed to Clean generated Archive.");
            }
        }
    }

    private void negotiateCredentialsLDAP() throws SubmitProjectException {
        try {
            /* Get LDAP Username and Password, Add it to Config */
            SwingUtilities.invokeAndWait(() -> {
                SubmitLDAPCredDialog credDialog = new SubmitLDAPCredDialog(this.project);
                credDialog.show();

                /* Dialog is Synchronous, Get Username and Password */
                this.config.put("loginName", credDialog.getUsername());
                this.config.put("password", credDialog.getPassword());
            });

            /* Define the Request */
            HttpPost request = new HttpPost(this.config.get("baseURL") + "/eclipse/NegotiateOneTimePassword");
            request.setHeader("Content-Type", "application/x-www-form-urlencoded");

            /* Define Post Parameters */
            List<NameValuePair> params = new ArrayList<>();
            String[] configParams = new String[]{ "loginName", "password", "courseKey", "projectNumber" };

            for (String param : configParams) {
                /* Get Param values from Config */
                params.add(new BasicNameValuePair(param, this.config.get(param)));
            }

            request.setEntity(new UrlEncodedFormEntity(params));
            CloseableHttpResponse response = httpClient.execute(request);
            String responseString = EntityUtils.toString(response.getEntity());

            /* Check if Authentication is Successful */
            if (responseString.startsWith("failed") || responseString.length() < 5)
                throw new SubmitProjectException("Invalid Username or Password");

            /* String consists of a Submit Server User Config File */
            List<String> rsConfig = Arrays.asList(responseString.split("\n"));
            parseSubmitServerConfig(rsConfig);

            Thread.sleep(250);
        } catch (Exception ex) {
            throw new SubmitProjectException("Authentication with Server Failed: " + ex.getMessage());
        }
    }

    private void parseSubmitServerConfig() throws SubmitProjectException {
        /* Update Status */
        this.progressIndicator.setText("Reading configuration files");

        try {
            /* Read All lines into String */
            List<String> submitServerConfig =
                    Files.readAllLines(
                            Path.of(this.project.getBasePath() + "/.submit"),
                            StandardCharsets.UTF_8
                    );

            /* Call Wrapper Method */
            parseSubmitServerConfig(submitServerConfig);

            /* Spoof Course Project Manager Version */
            this.config.put("submitClientVersion", "0.3.1");
            this.config.put("submitClientTool", "EclipsePlugin");
        } catch (IOException ex) {
            throw new SubmitProjectException("Unable to access the Submit Server configuration for this project.");
        }
    }

    private void parseSubmitServerConfig(List<String> submitServerConfig) throws SubmitProjectException {
        try {
            /* Parse Each Line of Config */
            for (String configLine : submitServerConfig) {
                /* Ignore comments (Lines startWith #) */
                if(!configLine.startsWith("#")) {
                    /* Key-Value Pairs are split with = */
                    String[] params = configLine.split("=");

                    /* Ensure the existence of a K-V Pair */
                    if (params.length == 2)
                        this.config.put(params[0], params[1]);
                }
            }

            /* Set Short Delay */
            Thread.sleep(500);
        } catch (Exception ex) {
            throw new SubmitProjectException("Failed to parse submit server configuration.");
        }
    }
}
