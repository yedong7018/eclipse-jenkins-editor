/*
 * Copyright 2017 Albert Tregnaghi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 */
package de.jcup.jenkinseditor.handlers;

import static de.jcup.jenkinseditor.JenkinsEditorConstants.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.IDocument;
import org.junit.FixMethodOrder;

import de.jcup.egradle.eclipse.util.EclipseUtil;
import de.jcup.jenkins.cli.JenkinsCLIConfiguration;
import de.jcup.jenkins.cli.JenkinsCLIConfiguration.AuthMode;
import de.jcup.jenkins.cli.JenkinsDefaultURLProvider;
import de.jcup.jenkins.cli.JenkinsLinterCLICommand;
import de.jcup.jenkins.cli.JenkinsLinterCLIResult;
import de.jcup.jenkins.linter.JenkinsLinterError;
import de.jcup.jenkins.linter.JenkinsLinterErrorBuilder;
import de.jcup.jenkinseditor.JenkinsEditor;
import de.jcup.jenkinseditor.JenkinsEditorActivator;
import de.jcup.jenkinseditor.JenkinsEditorLogSupport;
import de.jcup.jenkinseditor.JenkinsEditorMessageDialogSupport;
import de.jcup.jenkinseditor.JenkinsEditorUtil;
import de.jcup.jenkinseditor.preferences.JenkinsEditorPreferences;

public class CallLinterHandler extends AbstractJenkinsCLIHandler {

	private JenkinsLinterErrorBuilder errorBuilder = new JenkinsLinterErrorBuilder();
	private JenkinsDefaultURLProvider jenkinsDefaultURLprovider = new JenkinsDefaultURLProvider();
	private ConfigurationBuilder configBuilder = new ConfigurationBuilder();

	@Override
	protected void executeOnActiveJenkinsEditor(JenkinsEditor editor) {
		if (editor == null) {
			return;
		}
		IDocument document = editor.getDocument();
		if (document == null) {
			return;
		}
		String code = document.get();

		try {
			executeLinterFor(code, editor);
		} catch (IOException e) {
			editor.getLogSupport().logError("Lint call not possible", e);
		}
	}

	protected void executeLinterFor(String code, JenkinsEditor editor) throws IOException {
		JenkinsCLIConfiguration configuration = configBuilder.createConfiguration(jenkinsDefaultURLprovider);
		if (configuration == null) {
			return;
		}
		JenkinsLinterCLICommand command = new JenkinsLinterCLICommand();
		
		ProgressMonitorDialog dialog = new ProgressMonitorDialog(EclipseUtil.getActiveWorkbenchShell());
		IRunnableWithProgress runnable = new IRunnableWithProgress() {
			
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				monitor.beginTask("Validate script by Jenkins", IProgressMonitor.UNKNOWN);
				try {
					JenkinsLinterCLIResult result = command.execute(configuration, code);
					if (!result.wasCLICallSuccessFul()) {
						JenkinsEditorMessageDialogSupport.INSTANCE
								.showErrorWithDetails("Jenkins CLI call failed.", result.toString());
						return;
					}
					/* remove former linter errors (after call was possible ) */
					JenkinsEditorUtil.removeLinterErrors(editor);

					int errorCount =0;
					for (String line : result.getLines()) {
						JenkinsLinterError error = errorBuilder.build(line);
						if (error == null) {
							continue;
						}
						/* add linter error */
						JenkinsEditorUtil.addLinterError(editor, error);
						errorCount++;
					}

					if (errorCount>0) {
						JenkinsEditorMessageDialogSupport.INSTANCE.showWarning("This script has "+errorCount+" error(s) inside!");
					} else {
						JenkinsEditorMessageDialogSupport.INSTANCE
								.showInfo("This script has no failures.");
					}

				} catch (IOException e) {
					StringBuilder sb = new StringBuilder();
					sb.append("Linter action failed at '");
					sb.append(configuration.getJenkinsURL());
					sb.append("'\nMessage:");
					sb.append(e.getMessage());
					JenkinsEditorMessageDialogSupport.INSTANCE.showErrorWithDetails("IO error on calling linter",sb.toString());
					
					JenkinsEditorLogSupport.INSTANCE.logError(sb.toString(), e);
				} finally{
					monitor.done();
				}
				
			}
		};
		try {
			dialog.run(true, false, runnable);
		} catch (InvocationTargetException | InterruptedException e) {
			JenkinsEditorLogSupport.INSTANCE.logError("Linter execution failed", e);
		}
		
		
	}
	

}
