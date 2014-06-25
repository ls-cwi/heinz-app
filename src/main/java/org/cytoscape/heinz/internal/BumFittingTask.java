package org.cytoscape.heinz.internal;


import java.util.List;
import java.util.ArrayList;
import java.io.IOException;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.ListSingleSelection;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyRow;

/**
 * Task that fits a BUM model to a column of p-values in the node table.
 * 
 * The estimated parameters (lambda and a) are saved as columns in
 * the network table.
 */
public class BumFittingTask extends AbstractTask {
	
	private final CyTable nodeTable;
	private final CyTable networkTable;
	
	@Tunable(description="Fit BUM model parameters for network")
	public boolean fitBum = true;
	@Tunable(description="Node table column holding the p-values",
			 dependsOn="fitBum=true")
	public ListSingleSelection<String> pValueColumnName;
	@Tunable(description="Generate plots to evaluate the fit",
			 dependsOn="fitBum=true")
	public boolean showPlots = true;
	
	/**
	 * Initialise the task, getting the required tables.
	 */
    public BumFittingTask(
    		CyTable nodeTable,
    		CyTable networkTable) {
    	
    	// link to the TaskIterator
    	super();
    	
    	// set the tables as fields
    	this.nodeTable = nodeTable;
    	this.networkTable = networkTable;
    	
    	// Collect the names of the node table columns that have the type Double
    	List<String> doubleColumnNameList = new ArrayList<String>();
    	for (CyColumn column : nodeTable.getColumns()) {
    		if (column.getType() == Double.class) {
    			doubleColumnNameList.add(column.getName());
    		}
    	}
    	// Set the column names as options in the Tunable
    	pValueColumnName = new ListSingleSelection<String>(doubleColumnNameList);
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
		
		
		// Check if the p-value column consists of numbers between 0 and 1
		for (CyRow row : nodeTable.getAllRows()) {
			if (!row.isSet(pValueColumnName.getSelectedValue())) {
				throw new IllegalArgumentException(
						"p-value for node ‘" +
						row.get("name", String.class) +
						"’ missing.");
			}
			double pValue =  row.get(pValueColumnName.getSelectedValue(), Double.class);
			if (pValue < 0.0 || pValue > 1.0) {
				throw new IllegalArgumentException(
						"Invalid p-value for node ‘" +
						row.get("name", String.class) +
						"’.");
			}
		}
		
		//TODO
		
	}
}
