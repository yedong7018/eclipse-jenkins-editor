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
package de.jcup.jenkinseditor;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.ui.console.IConsolePageParticipant;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import de.jcup.eclipse.commons.PluginContextProvider;
import de.jcup.eclipse.commons.keyword.TooltipTextSupport;
import de.jcup.eclipse.commons.resource.EclipseResourceInputStreamProvider;
import de.jcup.egradle.eclipse.MainActivator;
import de.jcup.egradle.eclipse.util.ColorManager;
import de.jcup.egradle.eclipse.util.EclipseResourceHelper;
import de.jcup.jenkins.util.JenkinsLogAdapter;
import de.jcup.jenkinseditor.console.JenkinsEditorConsolePageParticipant;

/**
 * The activator class controls the plug-in life cycle
 */
public class JenkinsEditorActivator extends AbstractUIPlugin implements PluginContextProvider {

	// The plug-in COMMAND_ID
	public static final String PLUGIN_ID = "de.jcup.jenkinseditor.plugin"; //$NON-NLS-1$

	// The shared instance
	private static JenkinsEditorActivator plugin;
	private ColorManager colorManager;
	private Map<StyledText, IConsolePageParticipant> viewers = new HashMap<StyledText, IConsolePageParticipant>();
	private ServiceTracker<IProxyService, ?> proxyTracker;

	private JenkinsTaskTagsSupportProvider taskSupportProvider;

	/**
	 * The constructor
	 */
	public JenkinsEditorActivator() {
		colorManager = new ColorManager();
		taskSupportProvider = new JenkinsTaskTagsSupportProvider(this) ;
		TooltipTextSupport.setTooltipInputStreamProvider(new EclipseResourceInputStreamProvider(PLUGIN_ID));
		// setup log adapter with editor log support
		JenkinsLogAdapter.INSTANCE.delegatesTo(JenkinsEditorLogSupport.INSTANCE);

	}

	public ColorManager getColorManager() {
		return colorManager;
	}

	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		MainActivator.delegate(plugin);
		taskSupportProvider.getTodoTaskSupport().install();
	}

	public void stop(BundleContext context) throws Exception {
		plugin = null;
		colorManager.dispose();
		taskSupportProvider.getTodoTaskSupport().uninstall();
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static JenkinsEditorActivator getDefault() {
		return plugin;
	}

	public File getEmbeddedJenkinsCLIJarFile() throws IOException {
		File file = EclipseResourceHelper.DEFAULT.getFileInPlugin("lib/jenkins-cli.jar", PLUGIN_ID);
		return file;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public IProxyService getProxyService() {
		if (proxyTracker==null){
			BundleContext context = getBundle().getBundleContext();
			proxyTracker = new ServiceTracker(context, IProxyService.class, null);
			proxyTracker.open();
		}
		return (IProxyService) proxyTracker.getService();
	}

	@Override
	public AbstractUIPlugin getActivator() {
		return this;
	}

	@Override
	public String getPluginID() {
		return PLUGIN_ID;
	}
	
	public JenkinsTaskTagsSupportProvider getTaskSupportProvider() {
		return taskSupportProvider;
	}

    public void addViewer(StyledText viewer, JenkinsEditorConsolePageParticipant participant) {
        viewers.put(viewer, participant);
    }

    public void removeViewerWithPageParticipant(JenkinsEditorConsolePageParticipant participant) {
        Set<StyledText> toRemove = new HashSet<StyledText>();

        for (StyledText viewer : viewers.keySet()) {
            if (viewers.get(viewer) == participant){
                toRemove.add(viewer);
            }
        }

        for (StyledText viewer : toRemove){
            viewers.remove(viewer);
        }
        
    }

}
