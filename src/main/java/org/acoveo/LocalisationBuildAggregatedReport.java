package org.acoveo;

import hudson.maven.AbstractMavenBuild;
import hudson.maven.AggregatableAction;
import hudson.maven.ExecutedMojo;
import hudson.maven.MavenAggregatedReport;
import hudson.maven.MavenBuild;
import hudson.maven.MavenModule;
import hudson.maven.MavenModuleSet;
import hudson.maven.MavenModuleSetBuild;
import hudson.model.Action;
import hudson.model.AbstractBuild;
import hudson.tasks.test.TestResult;
import hudson.util.ChartUtil;
import hudson.util.ColorPalette;
import hudson.util.DataSetBuilder;
import hudson.util.Graph;
import hudson.util.ShiftedCategoryAxis;
import hudson.util.StackedAreaRenderer2;

import java.awt.Color;
import java.awt.Paint;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.acoveo.localisation_report.Language;
import org.acoveo.localisation_report.Report;
import org.acoveo.localisation_report.Report.Languages;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.StackedAreaRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.ui.RectangleInsets;

/** A build report for a multi module (i.e. maven) project
 * @author fhackenberger
 *
 */
public class LocalisationBuildAggregatedReport extends AbstractBuildReport<AbstractMavenBuild<?, ?>> implements MavenAggregatedReport, Serializable {
	private static final long serialVersionUID = -1208301771790814160L;
	private static final Logger log = Logger.getLogger(LocalisationBuildAggregatedReport.class.getName());

	transient Map<MavenBuild, LocalisationBuildIndividualReport> buildMap = new HashMap<MavenBuild, LocalisationBuildIndividualReport>();
	public LocalisationBuildAggregatedReport(AbstractMavenBuild<?, ?> build) {
		super(new ArrayList<LocalisationBuildReport>());
		setBuild(build);
	}
	
	@Override
	public synchronized void update(Map<MavenModule, List<MavenBuild>> moduleBuilds, MavenBuild newBuild) {
		log.info("update() " + moduleBuilds.size() + " modulesBuilds newBuild: " + newBuild.getModuleRoot() + " #" + newBuild.number + " " + newBuild.hashCode());
		for(MavenModule mod : moduleBuilds.keySet()) {
			log.info("module: " + mod.getName());
		}
//		StringBuilder executedMojos = new StringBuilder("executed mojos: ");
//		for(ExecutedMojo exMojo : newBuild.getExecutedMojos()) {
//			executedMojos.append(exMojo.groupId).append(":").append(exMojo.artifactId).append(":").append(exMojo.goal).append(" ");
//		}
//		log.info(executedMojos.toString());
		LocalisationBuildIndividualReport newReport = newBuild.getAction(LocalisationBuildIndividualReport.class);

		if (newReport != null) {
			// Aggregate the results
			LocalisationBuildIndividualReport oldReport = buildMap.get(newBuild);
			if(oldReport != null)
				removeReport(oldReport);
			addReport(newReport);
			buildMap.put(newBuild, newReport);
		}
	}
	
	void removeReport(LocalisationBuildIndividualReport newReport) {
		if(newReport.getResults() == null || newReport.getResults().isEmpty())
			return;
		Map<String, Language> langMap = createLangMap(getResults());
		for (LocalisationBuildReport report : newReport.getResults()) {
			if(report.getReport() == null || report.getReport().getLanguages() == null || report.getReport().getLanguages().getStringOrLanguage() == null)
				continue;
			for (Serializable langS : report.getReport().getLanguages().getStringOrLanguage()) {
				Language lang = (Language) langS;
				Language mergeLang = langMap.get(lang.getCode());
				if (mergeLang == null) {
					langMap.put(lang.getCode(), lang);
					continue;
				}
				long sumUnits = mergeLang.getUnits() - lang.getUnits();
				if(sumUnits <= 0) {
					langMap.remove(mergeLang.getCode());
					continue;
				}
				double sumPercent = (mergeLang.getTranslatedPercent() * mergeLang.getUnits() -
						lang.getTranslatedPercent() * lang.getUnits())
						/ sumUnits;
				mergeLang.setTranslatedPercent(sumPercent);
				mergeLang.setUnits(sumUnits);
			}
		}
		LocalisationBuildReport buildReport = createReport(langMap);
		getResults().clear();
		getResults().add(buildReport);
	}
	
	void addReport(LocalisationBuildIndividualReport newReport) {
		Collection<LocalisationBuildReport> reports = new LinkedList<LocalisationBuildReport>();
		reports.addAll(newReport.getResults());
		reports.addAll(getResults());
		Map<String, Language> langMap = createLangMap(reports);
		LocalisationBuildReport buildReport = createReport(langMap);
		getResults().clear();
		getResults().add(buildReport);
	}
	
	LocalisationBuildReport createReport(Map<String, Language> langMap) {
		LocalisationBuildReport buildReport = new LocalisationBuildReport();
		buildReport.setOwner(getBuild());
		if(!langMap.isEmpty()) {
			Report result = new Report();
			buildReport.setReport(result);
			Languages langs = new Languages();
			result.setLanguages(langs);
			langs.getStringOrLanguage().addAll(langMap.values());
		}
		return buildReport;
	}
	
	/** Maps from language code to Language ( #units, completion % ) */
	Map<String, Language> createLangMap(Collection<LocalisationBuildReport> reports) {
		// Maps from language code to Language ( #units, completion % )
		Map<String, Language> langMap = new HashMap<String, Language>();
		// Iterate over all LocalisationBuildReport and sum up the units and re-calculate the completion percentage
		for (LocalisationBuildReport report : reports) {
			if(report.getReport() == null || report.getReport().getLanguages() == null || report.getReport().getLanguages().getStringOrLanguage() == null)
				continue;
			for (Serializable langS : report.getReport().getLanguages().getStringOrLanguage()) {
				Language lang = (Language) langS;
				Language mergeLang = langMap.get(lang.getCode());
				if (mergeLang == null) {
					langMap.put(lang.getCode(), lang);
					continue;
				}
				long sumUnits = lang.getUnits() + mergeLang.getUnits();
				double sumPercent = (lang.getTranslatedPercent()
						* lang.getUnits() + mergeLang
						.getTranslatedPercent() * mergeLang.getUnits())
						/ sumUnits;
				mergeLang.setTranslatedPercent(sumPercent);
				mergeLang.setUnits(sumUnits);
			}
		}
		return langMap;
	}

	public Class<? extends AggregatableAction> getIndividualActionType() {
		return LocalisationBuildIndividualReport.class;
	}

	@Override
	public Action getProjectAction(MavenModuleSet moduleSet) {
		for (MavenModuleSetBuild build : moduleSet.getBuilds()) {
			if (build.getAction(LocalisationBuildAggregatedReport.class) != null) {
				return new LocalisationProjectAggregatedReport(moduleSet);
			}
		}
		// XXX Just for testing
		return new LocalisationProjectAggregatedReport(moduleSet);
	}
	

	public List<TestResult> getList(int start, int end) {
		List<TestResult> list = new ArrayList<TestResult>();
		LocalisationBuildReport firstResult = getFirstResult();
		if(firstResult == null)
			return list;
		if(end < 0)
			end = firstResult.getOwner().getParent().getBuilds().size();
		end = Math.min(end, firstResult.getOwner().getParent().getBuilds().size());
		for (AbstractBuild<?, ?> b : firstResult.getOwner().getParent().getBuilds().subList(start, end)) {
			if (b.isBuilding())
				continue;
			// TODO find the matching result in getResults()
//			TestResult o = getTotals().getResultInBuild(b);
//			if (o != null) {
//				list.add(o);
//			}
		}
		return list;
	}

	public List<TestResult> getList() {
		return getList(0, -1);
	}

	@Override
	public boolean isGraphActive() {
		// Maybe create a graph with bars, one for each language. I.e. an alternative view of reportDetail.jelly
		return false;
	}

	@Override
	public boolean isFloatingBoxActive() {
		return false;
	}

	// TODO This graph is pretty useless here, we copied the code to LocalisationProjectAggregatedReport. Replace this graph with something meaningful for a single build or remove the code
	public Graph getCountGraph() {
		return new GraphImpl("") {
			protected DataSetBuilder<String, ChartLabel> createDataSet() {
				DataSetBuilder<String, ChartLabel> data = new DataSetBuilder<String, ChartLabel>();

				Collection<LocalisationBuildReport> list = getResults();
//				try {
//					list = getList(Integer.parseInt(Stapler.getCurrentRequest()
//							.getParameter("start")), Integer.parseInt(Stapler
//							.getCurrentRequest().getParameter("end")));
//				} catch (NumberFormatException e) {
//					list = getList();
//				}

				for (LocalisationBuildReport report : list) {
					Languages langs = report.getReport().getLanguages();
					for (Serializable langS : langs.getStringOrLanguage()) {
						Language lang = (Language) langS;
						data.add(lang.getTranslatedPercent() * 100,
								lang.getCode(), new ChartLabel(report));
					}
				}

				return data;
			}
		};
	}

	private abstract class GraphImpl extends Graph {
		private final String yLabel;

		protected GraphImpl(String yLabel) {
			super(-1, 600, 300); // cannot use timestamp, since ranges may change
			this.yLabel = yLabel;
		}

		protected abstract DataSetBuilder<String, ChartLabel> createDataSet();

		protected JFreeChart createGraph() {
			final CategoryDataset dataset = createDataSet().build();

			final JFreeChart chart = ChartFactory.createStackedAreaChart(null, // chart
																				// title
					null, // unused
					yLabel, // range axis label
					dataset, // data
					PlotOrientation.VERTICAL, // orientation
					false, // include legend
					true, // tooltips
					false // urls
					);

			chart.setBackgroundPaint(Color.white);

			final CategoryPlot plot = chart.getCategoryPlot();

			// plot.setAxisOffset(new Spacer(Spacer.ABSOLUTE, 5.0, 5.0, 5.0,
			// 5.0));
			plot.setBackgroundPaint(Color.WHITE);
			plot.setOutlinePaint(null);
			plot.setForegroundAlpha(0.8f);
			// plot.setDomainGridlinesVisible(true);
			// plot.setDomainGridlinePaint(Color.white);
			plot.setRangeGridlinesVisible(true);
			plot.setRangeGridlinePaint(Color.black);

			CategoryAxis domainAxis = new ShiftedCategoryAxis(null);
			plot.setDomainAxis(domainAxis);
			domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_90);
			domainAxis.setLowerMargin(0.0);
			domainAxis.setUpperMargin(0.0);
			domainAxis.setCategoryMargin(0.0);

			final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
			ChartUtil.adjustChebyshev(dataset, rangeAxis);
			rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
			rangeAxis.setAutoRange(true);

			StackedAreaRenderer ar = new StackedAreaRenderer2() {
				@Override
				public Paint getItemPaint(int row, int column) {
					ChartLabel key = (ChartLabel) dataset.getColumnKey(column);
					if (key.getColor() != null)
						return key.getColor();
					return super.getItemPaint(row, column);
				}

				@Override
				public String generateURL(CategoryDataset dataset, int row,
						int column) {
					ChartLabel label = (ChartLabel) dataset
							.getColumnKey(column);
					return label.getUrl();
				}

				@Override
				public String generateToolTip(CategoryDataset dataset, int row,
						int column) {
					ChartLabel label = (ChartLabel) dataset
							.getColumnKey(column);
					return label.o.getOwner().getDisplayName();
					// TODO Add more information to the tooltip
					// + " : " + label.o.getDurationString();
				}
			};
			plot.setRenderer(ar);
			ar.setSeriesPaint(0, ColorPalette.RED); // Failures.
			ar.setSeriesPaint(1, ColorPalette.YELLOW); // Skips.
			ar.setSeriesPaint(2, ColorPalette.BLUE); // Total.

			// crop extra space around the graph
			plot.setInsets(new RectangleInsets(0, 0, 0, 5.0));

			return chart;
		}
	}

	class ChartLabel implements Comparable<ChartLabel> {
		LocalisationBuildReport o;
		String url;

		public ChartLabel(LocalisationBuildReport o) {
			this.o = o;
			this.url = null;
		}

		public String getUrl() {
			if (this.url == null)
				generateUrl();
			return url;
		}

		private void generateUrl() {
			AbstractBuild<?, ?> build = o.getOwner();
			String buildLink = build.getUrl();
			// TODO support the link properly
			// String actionUrl = o.getTestResultAction().getUrlName();
			// this.url = Hudson.getInstance().getRootUrl() + buildLink
			// + actionUrl + o.getUrl();
		}

		public int compareTo(ChartLabel that) {
			return this.o.getOwner().number - that.o.getOwner().number;
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof ChartLabel)) {
				return false;
			}
			ChartLabel that = (ChartLabel) o;
			return this.o == that.o;
		}

		public Color getColor() {
			return null;
		}

		@Override
		public int hashCode() {
			return o.hashCode();
		}

		@Override
		public String toString() {
			String l = o.getOwner().getDisplayName();
			String s = o.getOwner().getBuiltOnStr();
			if (s != null)
				l += ' ' + s;
			return l;
			// return o.getDisplayName() + " " + o.getOwner().getDisplayName();
		}

	}

}
