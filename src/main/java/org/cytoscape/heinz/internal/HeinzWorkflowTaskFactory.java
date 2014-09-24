
package org.cytoscape.heinz.internal;

import org.cytoscape.group.CyGroupFactory;
import org.cytoscape.group.CyGroupManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.task.AbstractNetworkTaskFactory;
import org.cytoscape.work.TaskIterator;

/**
 * TaskFactory for HeinzTask, to be provided as an OSGi service.
 */
public class HeinzWorkflowTaskFactory extends AbstractNetworkTaskFactory {

	private final CyGroupManager groupManager;
	private final CyGroupFactory groupFactory;
	
	public HeinzWorkflowTaskFactory(
			CyGroupManager groupManager, CyGroupFactory groupFactory) {
		super();
		this.groupManager = groupManager;
		this.groupFactory = groupFactory;
	}

	/**
	 * Create a TaskIterator with a new HeinzTask.
	 * <br />
	 * As this is an {@link org.cytoscape.task.NetworkTaskFactory}
	 * implementation, whoever uses this TaskFactory (e.g. a Swing
	 * MenuItem) and calls the getTask() method is assumed to have
	 * already called the setNetwork() method.
	 */
	public TaskIterator createTaskIterator(CyNetwork network) {
		return new TaskIterator(new HeinzWorkflowTask(network, groupManager, groupFactory));
	}
}
