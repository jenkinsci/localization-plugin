package hudson.plugins.helpers;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;

import hudson.model.HealthReportingAction;
import hudson.model.AbstractBuild;

/** A display class presenting results on the build page of a project
 * @author fhackenberger
 * @param <BUILD>
 */
public abstract class AbstractBuildAction<BUILD extends AbstractBuild<?, ?>> implements HealthReportingAction, Serializable {

	private BUILD build = null;

	protected AbstractBuildAction() {
	}

	public synchronized BUILD getBuild() {
		return build;
	}

	public synchronized void setBuild(BUILD build) {
		if (this.build == null && this.build != build) {
			this.build = build;
		}
	}

	public boolean isFloatingBoxActive() {
		return false;
	}

	public boolean isGraphActive() {
		return false;
	}

	public String getGraphName() {
		return getDisplayName();
	}

	public String getRootUrlEscaped() throws UnsupportedEncodingException {
		return AbstractProjectAction.getRootUrlEscapedStatic();
	}
	
	public String getSummary() {
		return "My Localisation Summary";
	}
}
