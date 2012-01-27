package hudson.plugins.helpers;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.servlet.http.HttpUtils;

import org.kohsuke.stapler.Stapler;

import hudson.model.AbstractProject;
import hudson.model.Actionable;

abstract public class AbstractProjectAction<PROJECT extends AbstractProject<?, ?>> extends Actionable {
	private final PROJECT project;

	protected AbstractProjectAction(PROJECT project) {
		this.project = project;
	}

	public PROJECT getProject() {
		return project;
	}

	public boolean isFloatingBoxActive() {
		return true;
	}

	public boolean isGraphActive() {
		return false;
	}

	public String getGraphName() {
		return getDisplayName();
	}
	
	public String getRootUrlEscaped() throws UnsupportedEncodingException {
		return getRootUrlEscapedStatic();
	}
	
	public static String getRootUrlEscapedStatic() throws UnsupportedEncodingException {
		return URLEncoder.encode(Stapler.getCurrentRequest().getRequestURL().toString(), "UTF-8");
	}
}