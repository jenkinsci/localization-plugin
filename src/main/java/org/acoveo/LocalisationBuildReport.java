package org.acoveo;

import hudson.model.AbstractBuild;

import java.io.Serializable;
import java.util.Collection;

import org.acoveo.localisation_report.Report;

/** Container for the data stored in build.xml
 * @author fhackenberger
 */
public class LocalisationBuildReport implements Serializable {
	private static final long serialVersionUID = -4199868418886539680L;
	Report report;
	AbstractBuild<?, ?> owner;
	public Report getReport() {
		return report;
	}
	public void setReport(Report report) {
		this.report = report;
	}
	public AbstractBuild<?, ?> getOwner() {
		return owner;
	}
	public void setOwner(AbstractBuild<?, ?> build) {
		this.owner = build;
	}
	public String toSummary(Collection<LocalisationBuildReport> prevBuildResults) {
		String result = "Summary:";
		if(report != null && report.getLanguages() != null && report.getLanguages().getStringOrLanguage() != null) {
			result += " " + report.getLanguages().getStringOrLanguage().size() + " languages.";
			if(prevBuildResults != null && !prevBuildResults.isEmpty())
				result += " Previous build: " + prevBuildResults.iterator().next().toSummary(null);
		}
		return result;
	}
}
