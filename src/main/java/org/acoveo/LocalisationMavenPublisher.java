package org.acoveo;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.maven.MavenBuildProxy;
import hudson.maven.MavenBuildProxy.BuildCallable;
import hudson.maven.MavenReporter;
import hudson.maven.MojoInfo;
import hudson.maven.MavenBuild;
import hudson.maven.MavenModule;
import hudson.model.Action;
import hudson.model.BuildListener;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Logger;

import net.sf.json.JSONObject;

import org.apache.maven.project.MavenProject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/** A maven reporter is a build listener on which callbacks are invoked during the build process
 * It is serialised between the master and slaves and is persisted in build.xml
 * @author fhackenberger
 *
 */
public class LocalisationMavenPublisher extends MavenReporter {
	private static final long serialVersionUID = -7835267547866169314L;
	private static final Logger log = Logger.getLogger(LocalisationMavenPublisher.class.getName());

	LocalisationBuildIndividualReport newAction = null;
	boolean mojoExecuted;

	@DataBoundConstructor
	public LocalisationMavenPublisher() {
	}

	private static final String PLUGIN_GROUP_ID = "org.acoveo";
	private static final String PLUGIN_ARTIFACT_ID = "localisation-maven-plugin";
	private static final String PLUGIN_EXECUTE_GOAL = "report";

	protected boolean isExecutingMojo(MojoInfo mojo) {
		log.fine(this.getClass().getSimpleName() + "#isExecutingMojo(): " + mojo.pluginName + " " + mojo.getGoal());
		return mojo.pluginName.matches(PLUGIN_GROUP_ID, PLUGIN_ARTIFACT_ID) && PLUGIN_EXECUTE_GOAL.equals(mojo.getGoal());
	}
	
	@Override
	public boolean postExecute(MavenBuildProxy build, MavenProject pom, MojoInfo mojo, BuildListener listener, Throwable error) throws InterruptedException, IOException {
		log.info(pom.getGroupId() + ":" + pom.getArtifactId() + ":" + mojo.getGoal() + " mojoExecuted: " + mojoExecuted);
		// If the report mojo has already been executed, don't do anything for other mojos
		// However, execute at least once, just in case the report mojo is not attached to
		// the lifecycle.
		if(mojoExecuted && !isExecutingMojo(mojo))
			return true;
		if(isExecutingMojo(mojo))
			mojoExecuted = true;
		newAction = new LocalisationGhostWriter(null).readReports(new FilePath(pom.getBasedir()));
		build.execute(new BuildCallable<Void, IOException>() {
			public Void call(MavenBuild build) throws IOException, InterruptedException {
				// Replace the existing action
				LocalisationBuildIndividualReport a = build.getAction(LocalisationBuildIndividualReport.class);
				if(a != null)
					build.getActions().remove(a);
				build.getActions().add(newAction);
				// Will call getProjectActions() later on
				build.registerAsProjectAction(LocalisationMavenPublisher.this);
				return null;
			}
		});
		newAction = null;
		return true;
	}

	@Override
	public boolean postBuild(MavenBuildProxy build, MavenProject pom, BuildListener listener) throws InterruptedException, IOException {
		log.info("postBuild " + pom.getGroupId() + ":" + pom.getArtifactId());
		// XXX We would like to attach AggregatableActions (LocalisationBuildIndividualReport) after
		// the whole build completed (here), but due to https://issues.jenkins-ci.org/browse/JENKINS-12475
		// that does not work.
		return true;
	}
	
	@Override
	public boolean end(MavenBuild build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
		// the member actions never ends up on the master, even though the docs on MavenReporter suggests
		// that the whole reporter is serialised back to the master. Therefore we have to use a BuildCallable
		
		// One exception is a maven build with only a single module. In that case the aggregatable
		// LocalisationBuildIndividualReport will not be aggregated automatically, so we have to do
		// that manually
//		List<Action> actions = build.getActions();
//		LocalisationBuildIndividualReport indiv = build.getAction(LocalisationBuildIndividualReport.class);
//		if(indiv != null) {
//			LocalisationBuildAggregatedReport aggreg = new LocalisationBuildAggregatedReport(build);
//			aggreg.getResults().addAll(indiv.getResults());
//			actions.remove(indiv);
//			actions.add(aggreg);
//		}
		log.info("end actions" + build.getActions());
		return true;
	}
	
	@Override
	public Collection<? extends Action> getProjectActions(MavenModule module) {
		List<LocalisationProjectIndividualReport> actions = new LinkedList<LocalisationProjectIndividualReport>();
		for (MavenBuild build : module.getBuilds()) {
			if (build.getAction(LocalisationBuildIndividualReport.class) != null) {
				actions.add(new LocalisationProjectIndividualReport(module));
			}
		}
		return actions;
	}

	@Extension
	public static final class DescriptorImpl extends hudson.maven.MavenReporterDescriptor {
		/**
		 * Do not instantiate DescriptorImpl.
		 */
		public DescriptorImpl() {
			super(LocalisationMavenPublisher.class);
		}

		/**
		 * {@inheritDoc}
		 */
		public String getDisplayName() {
			return "Publish " + LocalisationPlugin.DISPLAY_NAME;
		}

		public MavenReporter newInstance(StaplerRequest req, JSONObject formData) throws FormException {
			return req.bindJSON(LocalisationMavenPublisher.class, formData);
		}

	}

}
