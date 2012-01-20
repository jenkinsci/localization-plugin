package org.acoveo;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.maven.MavenModule;
import hudson.maven.MavenModuleSet;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import org.kohsuke.stapler.DataBoundConstructor;

public class LocalisationPublisher extends Recorder {
	private static final Logger log = Logger.getLogger(LocalisationPublisher.class.getName());
	private String reportFilenamePattern;

	@DataBoundConstructor
	public LocalisationPublisher(String reportFilenamePattern) {
		reportFilenamePattern.getClass();
		this.reportFilenamePattern = reportFilenamePattern;
	}

	public boolean needsToRunAfterFinalized() {
		return false;
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
		LocalisationBuildIndividualReport action = new LocalisationGhostWriter(reportFilenamePattern).readReports(new FilePath(build.getProject().getRootDir()));
		log.info("Adding 1 action to the build");
		build.getActions().add(action);
		return true;
	}

	@Override
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}

	@Extension
	public static final class DescriptorImpl extends hudson.tasks.BuildStepDescriptor<Publisher> {

		public DescriptorImpl() {
			super(LocalisationPublisher.class);
		}

		public String getDisplayName() {
			return "Publish " + LocalisationPlugin.DISPLAY_NAME;
		}

		public boolean isApplicable(Class<? extends AbstractProject> aClass) {
			return !MavenModuleSet.class.isAssignableFrom(aClass)
					&& !MavenModule.class.isAssignableFrom(aClass);
		}

	}

}
