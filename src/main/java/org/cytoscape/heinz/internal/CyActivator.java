package org.cytoscape.heinz.internal;

import java.util.Properties;

import org.cytoscape.service.util.AbstractCyActivator;
import org.osgi.framework.BundleContext;
import org.cytoscape.task.NetworkTaskFactory;
import org.cytoscape.group.CyGroupManager;
import org.cytoscape.group.CyGroupFactory;

import static org.cytoscape.work.ServiceProperties.*;
import static org.cytoscape.application.swing.ActionEnableSupport.ENABLE_FOR_NETWORK;


/**
 * Registers OSGi services accessible to Cytoscape.
 */
public class CyActivator extends AbstractCyActivator {
	
	/**
	 * Register a HeinzTaskFactory as an OSGi service for Cytoscape to find.
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		
		// get instances of required OSGi services
		CyGroupManager groupManager = getService(context, CyGroupManager.class);
		CyGroupFactory groupFactory = getService(context, CyGroupFactory.class);
		
		// create a task factory to provide as a service
		HeinzWorkflowTaskFactory taskFactory = new HeinzWorkflowTaskFactory(
				groupManager, groupFactory);
		
		// configure the properties for integrating the service into the GUI
		Properties properties = new Properties();
		properties.put(TITLE, "Run Heinz");
		properties.put(PREFERRED_MENU, "Apps");
		// Enable the menu button only when a network has been loaded
		properties.put(ENABLE_FOR, ENABLE_FOR_NETWORK);
		properties.put(TOOLTIP, "Identify functional modules based on p-values");
		
		registerService(context, taskFactory, NetworkTaskFactory.class, properties);
		
	}

}