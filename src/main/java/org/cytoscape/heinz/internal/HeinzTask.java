package org.cytoscape.heinz.internal;


import java.util.List;
import java.util.ArrayList;
import java.io.IOException;

import org.cytoscape.task.AbstractNetworkTask;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyNode;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.ListSingleSelection;
import org.cytoscape.work.util.BoundedDouble;


/**
 * Task that runs Heinz and adds a column to the network’s node table.
 */
public class HeinzTask extends AbstractNetworkTask {

	// Tunable parameters to be provided by the user before run() is called
	@Tunable(
			description="Mixture parameter (λ)",
			groups={"BUM model parameters"})
	public BoundedDouble lambda = new BoundedDouble(0.0, 0.5, 1.0, true, true);
	@Tunable(
			description="Shape parameter (a)",
			groups={"BUM model parameters"})
	public BoundedDouble a = new BoundedDouble(0.0, 0.25, 1.0, true, true);
	@Tunable(description="Node table column with p-values")
	public ListSingleSelection<String> pValueColumnName;
	@Tunable(description="False-discovery rate")
	public BoundedDouble fdr = new BoundedDouble(0.0, 0.01,	1.0, true, true);
	@Tunable(description="Host", groups={"Server details"})
	public String serverHost = "localhost";
	@Tunable(description="Port", groups={"Server details"})
	public int serverPort = 9000;
	@Tunable(description="Output column name")
	public String resultColumnName = "inHeinzModule";

	/**
	 * Initialise the task, getting a CyNetwork. 
	 * 
	 * @param n  the network to operate on
	 */
	public HeinzTask(final CyNetwork n) {
		
		// Will set a CyNetwork field called "network"
		super(n);
		
		// Collect the names of the node table columns that have the type Double
		List<String> doubleColumnNameList = new ArrayList<String>();
		for (CyColumn column : network.getDefaultNodeTable().getColumns()) {
			if (column.getType() == Double.class) {
				doubleColumnNameList.add(column.getName());
			}
		}
		// Set the column names as options in the Tunable
		pValueColumnName = new ListSingleSelection<String>(doubleColumnNameList);
		
	}

    /**
     * Run Heinz and add a column to the node table.
     * 
     * @throws IOException  if an error occurs communicating to Heinz
     */
	@Override
	public void run(final TaskMonitor taskMonitor)
			throws IOException {
		
		// TODO handle cancellation using if(cancelled) or overriding
		// cancel(), and calling a cleanup method
		
		// Give the task a title (shown in status monitor)
		taskMonitor.setTitle("Heinz");
		
		taskMonitor.setStatusMessage("Validating parameters");
		// Check if a p-value column has been set
		if (pValueColumnName.getSelectedValue() == null) {
			throw new IllegalArgumentException("No p-value column selected.");
		}
		// Check if the p-value column consists of numbers between 0 and 1
		for (CyRow row : network.getDefaultNodeTable().getAllRows()) {
			if (!row.isSet(pValueColumnName.getSelectedValue())) {
				throw new IllegalArgumentException(
						"p-value for node ‘" +
						row.get(CyNetwork.NAME, String.class) +
						"’ missing.");
			}
			double pValue =  row.get(pValueColumnName.getSelectedValue(), Double.class);
			if (pValue < 0.0 || pValue > 1.0) {
				throw new IllegalArgumentException(
						"Invalid p-value for node ‘" +
						row.get(CyNetwork.NAME, String.class) +
						"’.");
			}
		}
		taskMonitor.setProgress(0.02);
		
		taskMonitor.setStatusMessage("Connecting to the Heinz server");
		HeinzClient client = new SwHeinzClient(serverHost, serverPort);
		
		try {
			taskMonitor.setStatusMessage("Sending parameters to Heinz");
			client.sendLambda(lambda.getValue());
			client.sendA(a.getValue());
			client.sendFdr(fdr.getValue());

			taskMonitor.setStatusMessage("Sending node table to Heinz");
			client.sendNodeTable(
					network.getDefaultNodeTable(),
					pValueColumnName.getSelectedValue());
			taskMonitor.setProgress(0.06);

			taskMonitor.setStatusMessage("Sending edge table to Heinz");
			client.sendEdgeTable(network.getEdgeList());
			taskMonitor.setProgress(0.10);

			taskMonitor.setStatusMessage("Running Heinz");
			client.runHeinz();
			taskMonitor.setProgress(0.95);

			taskMonitor.setStatusMessage("Reading results into node table");
			// this writes to the local node table, specific to this subnetwork
			client.retrieveResults(
					network.getTable(CyNode.class, CyNetwork.LOCAL_ATTRS),
					resultColumnName);
			taskMonitor.setProgress(1.00);
			
		} finally {
			client.close();
		}
		
	}
}
