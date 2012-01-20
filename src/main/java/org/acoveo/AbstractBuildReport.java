package org.acoveo;

import hudson.model.HealthReport;
import hudson.model.AbstractBuild;
import hudson.plugins.helpers.AbstractBuildAction;

import java.io.Serializable;
import java.util.Collection;

import org.acoveo.localisation_report.Report;

public abstract class AbstractBuildReport<T extends AbstractBuild<?, ?>> extends AbstractBuildAction<T> implements Serializable {
	private static final long serialVersionUID = 1023976919569489941L;
	private final Collection<LocalisationBuildReport> results;

	public AbstractBuildReport(Collection<LocalisationBuildReport> results) {
		this.results = results;
//		this.totals = LocalisationBuildReport.total(results);
	}

	public Collection<LocalisationBuildReport> getResults() {
		return results;
	}
	
	public LocalisationBuildReport getFirstResult() {
		return (results == null || results.isEmpty()) ? null : results.iterator().next();
	}

	public String getSummary() {
		AbstractBuild<?, ?> prevBuild = getBuild().getPreviousBuild();
		while (prevBuild != null && prevBuild.getAction(getClass()) == null) {
			prevBuild = prevBuild.getPreviousBuild();
		}
		LocalisationBuildReport report = getFirstResult();
		if(report == null)
			return "";
		if (prevBuild == null) {
			return report.toSummary(null);
		} else {
			AbstractBuildReport action = prevBuild.getAction(getClass());
			return report.toSummary(action.getResults());
		}
	}

	public String getIconFileName() {
		return LocalisationPlugin.ICON_FILE_NAME;
	}

	public String getDisplayName() {
		return LocalisationPlugin.DISPLAY_NAME;
	}

	public String getUrlName() {
		return LocalisationPlugin.URL;
	}
	
	public boolean isReportEmpty() {
		return isResultsEmpty(getResults());
	}
	
	public static boolean isResultsEmpty(Collection<LocalisationBuildReport> results) {
		if(results != null) {
			for(LocalisationBuildReport bRep : results) {
				Report rep = bRep.getReport();
				if(rep != null && rep.getLanguages() != null && rep.getLanguages().getStringOrLanguage() != null)
					return false;
			}
		}
		return true;
	}

	public boolean isGraphActive() {
		return false;
	}

	@Override
	public HealthReport getBuildHealth() {
		return null;
	}

}