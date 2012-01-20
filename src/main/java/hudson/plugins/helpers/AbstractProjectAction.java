package hudson.plugins.helpers;

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
}