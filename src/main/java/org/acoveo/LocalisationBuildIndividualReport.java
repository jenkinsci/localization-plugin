package org.acoveo;

import hudson.maven.AggregatableAction;
import hudson.maven.MavenAggregatedReport;
import hudson.maven.MavenBuild;
import hudson.maven.MavenModule;
import hudson.maven.MavenModuleSetBuild;
import hudson.model.AbstractBuild;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/** A build report for a single module of a maven project or a non-maven project
 * The marker interface {@link AggregatableAction} tells jenkins that results for multi-module
 * builds should be aggregated by calling {@link #createAggregatedAction(MavenModuleSetBuild, Map)}.
 * @author fhackenberger
 *
 */
public class LocalisationBuildIndividualReport extends AbstractBuildReport<AbstractBuild<?, ?>> implements AggregatableAction, Serializable {
	private static final long serialVersionUID = 1552717836440119606L;
	private static final Logger log = Logger.getLogger(LocalisationBuildIndividualReport.class.getName());

	public LocalisationBuildIndividualReport(Collection<LocalisationBuildReport> results) {
		super(results);
	}

	@Override
	public synchronized void setBuild(AbstractBuild<?, ?> build) {
		super.setBuild(build);
		if (this.getBuild() != null) {
			for (LocalisationBuildReport r : getResults()) {
				r.setOwner(this.getBuild());
			}
		}
	}

	@Override
	public MavenAggregatedReport createAggregatedAction(MavenModuleSetBuild build, Map<MavenModule, List<MavenBuild>> moduleBuilds) {
		log.info("createAggregatedAction()");
		return new LocalisationBuildAggregatedReport(build);
	}
}
