package org.acoveo;

import java.io.Serializable;

import hudson.model.ProminentProjectAction;
import hudson.model.AbstractProject;

public class LocalisationProjectIndividualReport extends AbstractProjectReport<AbstractProject<?, ?>> implements ProminentProjectAction, Serializable {
	private static final long serialVersionUID = 6123908454925353116L;

	public LocalisationProjectIndividualReport(AbstractProject<?, ?> project) {
		super(project);
	}

	protected Class<? extends AbstractBuildReport> getBuildActionClass() {
		return LocalisationBuildIndividualReport.class;
	}
}
