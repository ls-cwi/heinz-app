package org.cytoscape.heinz.internal;


import java.io.IOException;

import org.cytoscape.task.AbstractNetworkTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyNode;


/**
 * Task that runs Heinz and adds a column to the network’s node table.
 */
public class HeinzTask extends AbstractNetworkTask {
	
	private final String pValueColumnName;
	private final String resultColumnName;
	private final double fdr;
	private final String serverHost;
	private final int serverPort;
	private Double lambda = null;
	private Double a = null;
	
	/**
	 * Initialise the task, setting the required parameters as fields.
	 * 
	 * @param network  the CyNetwork to detect a module in
	 * @param pValueColumnName  the node table column holding the p-values
	 * @param resultColumnName  the node table column to write the results to
	 * @param fdr  the false discovery rate
	 * @param lambda  the BUM model mixture parameter or null
	 * @param a  the BUM model shape parameter or null
	 * @param serverHost  the host name of the Heinz server
	 * @param serverPort  the port number of the Heinz server
	 */
	public HeinzTask(
			CyNetwork network,
			String pValueColumnName,
			String resultColumnName,
			double fdr,
			Double lambda,
			Double a,
			String serverHost,
			int serverPort) {
		// The superclass constructor will set the network field
		super(network);
		if (pValueColumnName == null) {
			throw new IllegalArgumentException(
					"No p-value column name.");
		}
		this.pValueColumnName = pValueColumnName;
		if (resultColumnName == null) {
			throw new IllegalArgumentException(
					"No Heinz result column name.");
		}
		this.resultColumnName = resultColumnName;
		if (!(fdr > 0.0 && fdr < 1.0)) {
			throw new IllegalArgumentException(
					"FDR parameter out of range.");
		}
		this.fdr = fdr;
		this.lambda = lambda;
		this.a = a;
		this.serverHost = serverHost;
		this.serverPort = serverPort;
	}
	
    /**
     * Run Heinz and add a column to the node table.
     * 
     * @throws IOException  if an error occurs communicating to Heinz
     */
	@Override
	public void run(final TaskMonitor taskMonitor)
			throws IOException {
		
		// Give the task a title (shown in status monitor)
		taskMonitor.setTitle("Heinz");
		
		taskMonitor.setStatusMessage("Validating parameters");
		// Check if the p-value column consists of numbers between 0 and 1
		for (CyRow row : network.getDefaultNodeTable().getAllRows()) {
			if (!row.isSet(pValueColumnName)) {
				throw new IllegalArgumentException(
						"p-value for node ‘" +
						row.get(CyNetwork.NAME, String.class) +
						"’ missing.");
			}
			double pValue =  row.get(pValueColumnName, Double.class);
			if (!(pValue > 0.0 && pValue < 1.0)) {
				throw new IllegalArgumentException(
						"Invalid p-value for node ‘" +
						row.get(CyNetwork.NAME, String.class) +
						"’.");
			}
		}
		taskMonitor.setProgress(0.02);
		
		// stop if Cancel was clicked
		if (cancelled) { return; }
		
		taskMonitor.setStatusMessage("Connecting to the Heinz server");
		HeinzClient client = new SwHeinzClient(serverHost, serverPort);
		
		try {
			
			// skip to `finally` and stop if Cancel was clicked
			if (cancelled) { return; }
			
			taskMonitor.setStatusMessage("Sending parameters to Heinz");
			client.sendLambda(lambda);
			client.sendA(a);
			client.sendFdr(fdr);
			
			// skip to `finally` and stop if Cancel was clicked
			if (cancelled) { return; }

			taskMonitor.setStatusMessage("Sending node table to Heinz");
			client.sendNodeTable(
					network.getDefaultNodeTable(),
					pValueColumnName);
			taskMonitor.setProgress(0.06);
			
			// skip to `finally` and stop if Cancel was clicked
			if (cancelled) { return; }
			
			taskMonitor.setStatusMessage("Sending edge table to Heinz");
			client.sendEdgeTable(network.getEdgeList());
			taskMonitor.setProgress(0.10);
			
			// skip to `finally` and stop if Cancel was clicked
			if (cancelled) { return; }

			taskMonitor.setStatusMessage("Running Heinz");
			client.runHeinz();
			taskMonitor.setProgress(0.95);
			
			// skip to `finally` and stop if Cancel was clicked
			if (cancelled) { return; }

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
