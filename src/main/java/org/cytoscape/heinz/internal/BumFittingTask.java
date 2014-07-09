package org.cytoscape.heinz.internal;


import java.io.IOException;

import org.cytoscape.task.AbstractTableColumnTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyColumn;


/**
 * Task that fits a BUM model to a column of p-values in the node table.
 * 
 * The estimated parameters (lambda and a) are saved as columns in
 * the network table.
 */
public class BumFittingTask extends AbstractTableColumnTask {
	
	private final CyTable networkTable;
	private final String serverHost;
	private final int serverPort;
	
	@Tunable(description="Generate plots to evaluate the fit")
	public boolean showPlots = true;
	
	/**
	 * Initialise the task, obtaining required parameters.
	 * 
	 * @param pValueColumn  node table column holding the p-values to fit to
	 * @param networkTable  network table to write the results to
	 * @param serverHost  the host name of the model fitting server
	 * @param serverPort  the port number of the model fitting server
	 */
    public BumFittingTask(
    		CyColumn pValueColumn,
    		CyTable networkTable,
    		String serverHost,
    		int serverPort) {
    	// set the `column' field
    	super(pValueColumn);
    	// set the other parameters as fields
    	if (networkTable == null) {
    		throw new IllegalArgumentException(
    				"No network table to write the BUM parameters to.");
    	}
    	this.networkTable = networkTable;
    	this.serverHost = serverHost;
    	this.serverPort = serverPort;	
    }

	/**
     * Fit a BUM model and write the parameters to the network table.
     * 
     * @throws IOException  if an error occurs communicating to the server
     */
	@Override
	public void run(final TaskMonitor taskMonitor)
			throws IOException {
		
		// Give the task a title (shown in status monitor)
		taskMonitor.setTitle("BUM Model Fitting");
		
		//TODO
		
	}
}