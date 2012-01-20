package org.acoveo;

import hudson.maven.MavenModuleSet;
import hudson.model.ProminentProjectAction;
import hudson.model.AbstractBuild;
import hudson.model.Hudson;
import hudson.util.ChartUtil;
import hudson.util.DataSetBuilder;
import hudson.util.Graph;
import hudson.util.ShiftedCategoryAxis;

import java.awt.Color;
import java.awt.Paint;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.acoveo.localisation_report.Language;
import org.acoveo.localisation_report.Report.Languages;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.CategoryToolTipGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.DefaultCategoryItemRenderer;
import org.jfree.chart.urls.CategoryURLGenerator;
import org.jfree.data.category.CategoryDataset;
import org.jfree.ui.RectangleInsets;

/** This report sums up the localisation information from several builds, each with a
 * {@link LocalisationBuildAggregatedReport}.
 * @author fhackenberger
 *
 */
public class LocalisationProjectAggregatedReport extends AbstractProjectReport<MavenModuleSet> implements ProminentProjectAction, Serializable {
	private static final long serialVersionUID = 7610515713614319923L;

	public LocalisationProjectAggregatedReport(MavenModuleSet project) {
		super(project);
	}

	protected Class<? extends AbstractBuildReport> getBuildActionClass() {
		return LocalisationBuildAggregatedReport.class;
	}
	
	@Override
	public boolean isGraphActive() {
		boolean empty = true;
		for(LocalisationBuildAggregatedReport rep : getList()) {
			if(!AbstractBuildReport.isResultsEmpty(rep.getResults())) {
				empty = false;
				break;
			}
		}
		return !empty;	
	}

	@Override
	public boolean isFloatingBoxActive() {
		return true;
	}
	
	public List<LocalisationBuildAggregatedReport> getList(int start, int end) {
		List<LocalisationBuildAggregatedReport> list = new ArrayList<LocalisationBuildAggregatedReport>();
		end = Math.min(end, getProject().getBuilds().size());
		for (AbstractBuild<?, ?> b : getProject().getBuilds().subList(start, end)) {
			if (b.isBuilding())
				continue;
			// TODO Could that also be a LocalisationBuildIndividualReport??
			LocalisationBuildAggregatedReport report = b.getAction(LocalisationBuildAggregatedReport.class);
			if(report != null)
				list.add(b.getAction(LocalisationBuildAggregatedReport.class));
		}
		return list;
	}

	public List<LocalisationBuildAggregatedReport> getList() {
		return getList(0, getProject().getBuilds().size());
	}
	
	public Graph getCountGraph() {
		return new GraphImpl("") {
			protected DataSetBuilder<String, ChartLabel> createDataSet() {
				DataSetBuilder<String, ChartLabel> data = new DataSetBuilder<String, ChartLabel>();

				Collection<LocalisationBuildAggregatedReport> list = getList();
				// TODO support specifying the start and end parameters for the build numbers
//				try {
//					list = getList(Integer.parseInt(Stapler.getCurrentRequest()
//							.getParameter("start")), Integer.parseInt(Stapler
//							.getCurrentRequest().getParameter("end")));
//				} catch (NumberFormatException e) {
//					list = getList();
//				}

				for (LocalisationBuildAggregatedReport report : list) {
					LocalisationBuildReport firstResult = report.getFirstResult();
					if(firstResult == null)
						continue;
					Languages langs = firstResult.getReport().getLanguages();
					for (Serializable langS : langs.getStringOrLanguage()) {
						Language lang = (Language) langS;
						data.add(lang.getTranslatedPercent() * 100,
								lang.getCode(), /* The chart series key */
								new ChartLabel(report) /* The label for the column (build number) */);
					}
				}

				return data;
			}
		};
	}

	/** A graph showing multiple series ( x-y lines ), each corresponding to one translated
	 * language. The x axis is the build number, the y axis the percentage of completion of the
	 * translation.
	 * 
	 * @author fhackenberger
	 *
	 */
	private abstract class GraphImpl extends Graph {
		private final String yLabel;

		protected GraphImpl(String yLabel) {
			super(-1, 500, 300); // cannot use timestamp, since ranges may change
			this.yLabel = yLabel;
		}

		protected abstract DataSetBuilder<String, ChartLabel> createDataSet();

		protected JFreeChart createGraph() {
			final CategoryDataset dataset = createDataSet().build();

			final JFreeChart chart = ChartFactory.createStackedAreaChart(null, // chart title
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
			rangeAxis.setRange(0, 105);

			DefaultCategoryItemRenderer renderer = new DefaultCategoryItemRenderer() {
				@Override
				public Paint getItemPaint(int row, int column) {
					ChartLabel key = (ChartLabel) dataset.getColumnKey(column);
					if (key.getColor() != null)
						return key.getColor();
					return super.getItemPaint(row, column);
				}
			};
			renderer.setBaseItemURLGenerator(new CategoryURLGenerator() {
				
				@Override
				public String generateURL(CategoryDataset dataset, int series, int category) {
					ChartLabel label = (ChartLabel) dataset.getColumnKey(category);
					return label.getUrl();
				}
			});
			renderer.setBaseToolTipGenerator(new CategoryToolTipGenerator() {
				@Override
				public String generateToolTip(CategoryDataset dataset, int row, int column) {
					ChartLabel label = (ChartLabel) dataset.getColumnKey(column);
					return dataset.getRowKey(row) + " " + label.o.getBuild().getDisplayName();
				}
			});
			plot.setRenderer(renderer);

			// crop extra space around the graph
			plot.setInsets(new RectangleInsets(0, 0, 0, 5.0));

			return chart;
		}
	}

	class ChartLabel implements Comparable<ChartLabel> {
		LocalisationBuildAggregatedReport o;
		String url;

		public ChartLabel(LocalisationBuildAggregatedReport o) {
			this.o = o;
			this.url = null;
		}

		public String getUrl() {
			if (this.url == null)
				generateUrl();
			return url;
		}

		private void generateUrl() {
			AbstractBuild<?, ?> build = o.getBuild();
			String buildLink = build.getUrl();
			String actionUrl = o.getUrlName();
			this.url = Hudson.getInstance().getRootUrl() + buildLink + actionUrl;
		}

		public int compareTo(ChartLabel that) {
			return this.o.getBuild().number - that.o.getBuild().number;
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
			String l = o.getBuild().getDisplayName();
			String s = o.getBuild().getBuiltOnStr();
			if (s != null)
				l += ' ' + s;
			return l;
		}

	}
}
