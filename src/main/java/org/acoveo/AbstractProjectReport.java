package org.acoveo;

import hudson.model.ProminentProjectAction;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.plugins.helpers.AbstractProjectAction;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;

public abstract class AbstractProjectReport<T extends AbstractProject<?, ?>> extends AbstractProjectAction<T> implements ProminentProjectAction, Serializable {
	private static final long serialVersionUID = -1574233100165143407L;

	public AbstractProjectReport(T project) {
		super(project);
	}
	
	public String getSummary() {
		return "Summary";
	}

	public String getIconFileName() {
		for (AbstractBuild<?, ?> build = getProject().getLastBuild(); build != null; build = build
				.getPreviousBuild()) {

			final AbstractBuildReport action = build
					.getAction(getBuildActionClass());
			if (action != null) {
				return LocalisationPlugin.ICON_FILE_NAME;
			}
		}
		return null;
	}

	public String getDisplayName() {
		for (AbstractBuild<?, ?> build = getProject().getLastBuild(); build != null; build = build
				.getPreviousBuild()) {
			final AbstractBuildReport action = build
					.getAction(getBuildActionClass());
			if (action != null) {
				return LocalisationPlugin.DISPLAY_NAME;
			}
		}
		return null;
	}

	public String getUrlName() {
		for (AbstractBuild<?, ?> build = getProject().getLastBuild(); build != null; build = build
				.getPreviousBuild()) {
			final AbstractBuildReport action = build
					.getAction(getBuildActionClass());
			if (action != null) {
				return LocalisationPlugin.URL;
			}
		}
		return null;
	}
	
	public String getSearchUrl() {
		return LocalisationPlugin.URL;
	}
	
	public boolean isReportEmpty() {
		return AbstractBuildReport.isResultsEmpty(getResults());
	}
	
	public boolean isGraphActive() {
		return false;
	}

	public Collection<LocalisationBuildReport> getResults() {
		for (AbstractBuild<?, ?> build = getProject().getLastBuild(); build != null; build = build.getPreviousBuild()) {
			final AbstractBuildReport action = build.getAction(getBuildActionClass());
			if (action != null) {
				return action.getResults();
			}
		}
		return Collections.emptySet();
	}

	protected abstract Class<? extends AbstractBuildReport> getBuildActionClass();
}
