package org.cytoscape.heinz.internal;


import java.util.List;
import java.io.IOException;

import java.awt.Window;
import javax.swing.JDialog;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

import org.cytoscape.task.AbstractNetworkTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyRow;
import org.cytoscape.group.CyGroupManager;


/**
 * Task that fits a BUM model to a column of p-values in the node table.
 * 
 * The estimated parameters (lambda and a) are saved as columns in
 * the network table.
 */
public class BumFittingTask extends AbstractNetworkTask {
	
	private final String pValueColumnName;
	private final int starts;
	private final String serverHost;
	private final int serverPort;
	private final CyGroupManager groupManager;
	
	@Tunable(description="Generate plots to evaluate the fit")
	public boolean showPlots = true;

	
	/**
	 * Initialise the task, obtaining required parameters.
	 * 
	 * @param network  the network to work on
	 * @param pValueColumnName  name of the node table column holding the p-values to fit to
	 * @param starts  number of starts for model fitting
	 * @param networkTableRow  network table row to write the results to
	 * @param serverHost  the host name of the model fitting server
	 * @param serverPort  the port number of the model fitting server
	 * @param groupManager  an instance of CyGroupManager
	 */
    public BumFittingTask(
    		CyNetwork network,
    		String pValueColumnName,
    		int starts,
    		String serverHost,
    		int serverPort,
    		CyGroupManager groupManager) {
    	// set the `network' field
    	super(network);
    	// set the other parameters as fields
    	if (network.getDefaultNodeTable().getColumn(pValueColumnName) == null) {
    		throw new IllegalArgumentException(
    				"Column ‘" +
    				pValueColumnName +
    				"’ does not exist in node table.");
    	}
    	this.pValueColumnName = pValueColumnName;
    	this.starts = starts;
    	this.serverHost = serverHost;
    	this.serverPort = serverPort;
    	if (groupManager == null) {
    		throw new IllegalArgumentException(
    				"No GroupManager received by BumFittingTask");
    	}
    	this.groupManager = groupManager;
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
		
		BumFitterClient client = new SwBumFitterClient(serverHost, serverPort);
		
		try {
			
			taskMonitor.setStatusMessage(
					"Sending p-values to the model fitting server");
			// read the p-values for all nodes into an array
			List<CyNode> nodeList = network.getNodeList();
			double[] pValueArray = new double[network.getNodeCount()];
			// test for something that should never happen
			if (nodeList.size() != pValueArray.length) {
				throw new RuntimeException(
						"Number of nodes in node list is not equal to node count of the network");
			}
			// index both the node list and the p-value array
			for (int i = 0; i < pValueArray.length; ++i) {
				Double pValue = TableUtils.getNodeAttribute(
						nodeList.get(i),
						network,
						pValueColumnName,
						Double.class,
						groupManager);
				if (!(pValue >= 0.0 && pValue <= 1.0)) {
					throw new IllegalArgumentException(
							"Invalid p-value for node ‘" +
							TableUtils.getNodeAttribute(
									nodeList.get(i),
									network,
									"name",
									String.class,
									groupManager) +
							"’.");
				}
				pValueArray[i] = pValue.doubleValue();
			}
			// send the p-values to the server
			client.sendPValues(pValueArray);

			taskMonitor.setStatusMessage(
					"Sending settings to the model fitting server");
			client.sendStarts(starts);
			if (showPlots) {
				client.enablePlotting();
			}

			taskMonitor.setStatusMessage(
					"Fitting a BUM model to the p-values");
			client.run();

			taskMonitor.setStatusMessage(
					"Writing fitted BUM model parameters to the network table");
			CyTable networkTable = network.getDefaultNetworkTable();
			CyRow networkTableRow = networkTable.getRow(network.getSUID());

			String lambdaColumnName = pValueColumnName + ".BUM.lambda";
			// if the column does not yet exist in the network table 
			if (networkTable.getColumn(lambdaColumnName) == null) {
				// create the column
				networkTable.createColumn(lambdaColumnName, Double.class, false);
				// if the lambda column does already exist
			} else {
				// check if the column has the correct type
				if (networkTable.getColumn(lambdaColumnName).getType() !=
						Double.class) {
					throw new IllegalArgumentException(
							"Network table column " +
									lambdaColumnName +
							" is not of type Double.");
				}
			}
			// write the fitted value to the network table
			networkTableRow.set(lambdaColumnName, client.getLambda());

			String aColumnName = pValueColumnName + ".BUM.a";
			// if the column does not yet exist in the network table 
			if (networkTable.getColumn(aColumnName) == null) {
				// create the column
				networkTable.createColumn(aColumnName, Double.class, false);
				// if the a column does already exist
			} else {
				// check if the column has the correct type
				if (networkTable.getColumn(aColumnName).getType() !=
						Double.class) {
					throw new IllegalArgumentException(
							"Network table column " +
									aColumnName +
							" is not of type Double.");
				}
			}
			// write the fitted value to the network table
			networkTableRow.set(aColumnName, client.getA());

			if (showPlots) {
				taskMonitor.setStatusMessage("Displaying plots");
				byte[] pngFileContents = client.getPlotPng();
				// create a dialog window with a hidden owner
				JDialog window = new JDialog(
						(Window) null,
						"BUM model fit evaluation plots");
				Icon icon = new ImageIcon(
						pngFileContents,
						"BUM model fit evaluation plots");
				window.getContentPane().add(new JLabel(icon), JLabel.CENTER);
				window.pack();
				window.setVisible(true);
			}
			
		} finally {
			client.close();
		}
		
	}
}
