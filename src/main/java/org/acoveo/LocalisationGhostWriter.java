package org.acoveo;

import hudson.FilePath;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.acoveo.localisation_report.Language;
import org.acoveo.localisation_report.ObjectFactory;
import org.acoveo.localisation_report.Report;
import org.acoveo.localisation_report.Report.Languages;

public class LocalisationGhostWriter {
	private static final Logger log = Logger.getLogger(LocalisationGhostWriter.class.getName());
	private String reportFilenamePattern = "**/localisation-reports/report.xml";

	public LocalisationGhostWriter(String reportFilenamePattern) {
		if(reportFilenamePattern != null)
			this.reportFilenamePattern = reportFilenamePattern;
	}

	public LocalisationBuildIndividualReport readReports(FilePath rootDir) throws InterruptedException, IOException {
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance("org.acoveo.localisation_report", ObjectFactory.class.getClassLoader());
			Unmarshaller unMarshaller = jaxbContext.createUnmarshaller();
	
			log.info("Searching for file pattern " + reportFilenamePattern + " in " + rootDir);
			FilePath[] paths = rootDir.list(reportFilenamePattern);
			Collection<LocalisationBuildReport> results = new LinkedList<LocalisationBuildReport>();
			Set<String> parsedFiles = new HashSet<String>();
			for (FilePath path : paths) {
				final String pathStr = path.getRemote();
				if (!parsedFiles.contains(pathStr)) {
					log.info("Reading localisation report from: " + pathStr);
					Report report = (Report)unMarshaller.unmarshal(new File(pathStr));
					parsedFiles.add(pathStr);
					LocalisationBuildReport buildReport = new LocalisationBuildReport();
					buildReport.setReport(report);
					results.add(buildReport);
				}
			}
	
			log.info("Found " + results.size() + " report results");
			LocalisationBuildIndividualReport action = new LocalisationBuildIndividualReport(results);
//			if (targets != null && targets.length > 0) {
//				HealthReport r = null;
//					for (JavaNCSSHealthTarget target : targets) {
//					r = HealthReport.min(r, target.evaluateHealth(action));
//				}
//				action.setBuildHealth(r);
//			}
			return action;
		}catch (Exception e) {
			throw new IOException(MessageFormat.format("Failed to parse reports", e), e);
		}
	}
}