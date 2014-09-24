package org.cytoscape.heinz.internal;


import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.io.IOException;

import org.cytoscape.task.AbstractNetworkTask;
import org.cytoscape.group.CyGroupFactory;
import org.cytoscape.group.CyGroupManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyRow;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.ListSingleSelection;
import org.cytoscape.work.util.BoundedDouble;


/**
 * Task that sets up a Heinz workflow.
 * 
 *  This involves obtaining parameters and appending the appropriate tasks
 *  to the calling task iterator.
 */
public class HeinzWorkflowTask extends AbstractNetworkTask {
	
	private final CyGroupManager groupManager;
	private final CyGroupFactory groupFactory;

	// Tunable parameters to be prompted for before run() is called
	@Tunable(
			description="Fit BUM  model parameters",
			groups={"BUM model"})
	public boolean fitBum = true;
	@Tunable(
			description="Mixture parameter (λ)",
			groups={"BUM model"},
			dependsOn="fitBum=false")
	public BoundedDouble lambda = new BoundedDouble(0.0, 0.5, 1.0, true, true);
	@Tunable(
			description="Shape parameter (a)",
			groups={"BUM model"},
			dependsOn="fitBum=false")
	public BoundedDouble a = new BoundedDouble(0.0, 0.25, 1.0, true, true);
	@Tunable(
			description="Host",
			groups={"BUM model", "Parameter fitting", "server"},
			dependsOn="fitBum=true")
	public String bumServerHost = "localhost";
	@Tunable(
			description="Port",
			groups={"BUM model", "Parameter fitting", "server"},
			dependsOn="fitBum=true")
	public int bumServerPort = 9000;
	@Tunable(
			description="Number of starts for model fitting",
			groups={"BUM model", "Parameter fitting"},
			dependsOn="fitBum=true")
	public int bumFittingStarts = 10;
	

	@Tunable(
			description="False-discovery rate",
			groups = {"Heinz"})
	public BoundedDouble fdr = new BoundedDouble(0.0, 0.01,	1.0, true, true);
	@Tunable(
			description="Host",
			groups={"Heinz", "Heinz server"})
	public String heinzServerHost = "localhost";
	@Tunable(
			description="Port",
			groups={"Heinz", "Heinz server"})
	public int heinzServerPort = 9001;
	
	@Tunable(
			description="Node table column holding the p-values",
			groups={"General"})
	public ListSingleSelection<String> pValueColumnName;
	@Tunable(
			description="Output column name",
			groups={"General"})
	public String resultColumnName = "in Heinz module";
	
	@Tunable(
			description="Perform GO enrichment",
			groups={"GO Enrichment"})
	public Boolean performGoEnrichment = true;
	@Tunable(
			description="BridgeDB Derby database file (http://bridgedb.org/data/gene_database/)",
			groups={"GO Enrichment"},
			dependsOn="performGoEnrichment=true",
			params="input=true")
	public File bridgeDbFile = null;
	@Tunable(
			description="Gene ID column",
			groups={"GO Enrichment"},
			dependsOn="performGoEnrichment=true")
	public ListSingleSelection<String> idColumnSelector;
	@Tunable(
			description="Gene ID type",
			groups={"GO Enrichment"},
			dependsOn="performGoEnrichment=true")
	public ListSingleSelection<String> idTypeSelector;
	
	/**
	 * Initialise the task, getting a CyNetwork.
	 * 
	 * @param n  the network to operate on
	 */
	public HeinzWorkflowTask(
			final CyNetwork n,
			final CyGroupManager groupManager,
			final CyGroupFactory groupFactory) {
		
		// Will set a CyNetwork field called "network"
		super(n);
		
		this.groupManager = groupManager;
		this.groupFactory = groupFactory;
		
		// Collect the names of the node table columns that have the type Double
		List<String> doubleColumnNameList = new ArrayList<String>();
		for (CyColumn column : network.getDefaultNodeTable().getColumns()) {
			if (column.getType() == Double.class) {
				doubleColumnNameList.add(column.getName());
			}
		}
		// Set the column names as options in the Tunable
		pValueColumnName = new ListSingleSelection<String>(doubleColumnNameList);
		
		// Collect the names of the node table columns that have the type String
		List<String> stringColumnNameList = new ArrayList<String>();
		for (CyColumn column : network.getDefaultNodeTable().getColumns()) {
			if (column.getType() == String.class) {
				stringColumnNameList.add(column.getName());
			}
		}
		// Set the column names as options in the Tunable
		idColumnSelector = new ListSingleSelection<String>(stringColumnNameList);
		
		// List the supported ID types in the ID type selector
		String[] supportedIdTypes = GoEnrichmentTask.getSupportedIdTypes();
		java.util.Arrays.sort(supportedIdTypes);
		idTypeSelector = new ListSingleSelection<String>(supportedIdTypes);
		
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
		taskMonitor.setTitle("Heinz Workflow");
		
		taskMonitor.setStatusMessage("Validating parameters");
		// Check if a p-value column has been set
		if (pValueColumnName.getSelectedValue() == null) {
			throw new IllegalArgumentException("No p-value column selected.");
		}
		// Check if the p-value column consists of numbers between 0 and 1
		for (CyRow row : network.getDefaultNodeTable().getAllRows()) {
			// stop if Cancel was clicked
			if (cancelled) { return; }
			// test if a p-value exists for this row
			if (!row.isSet(pValueColumnName.getSelectedValue())) {
				throw new IllegalArgumentException(
						"p-value for node ‘" +
						row.get(CyNetwork.NAME, String.class) +
						"’ missing.");
			}
			// test if the p-value is in the valid range
			double pValue =  row.get(pValueColumnName.getSelectedValue(), Double.class);
			if (!(pValue >= 0.0 && pValue <= 1.0)) {
				throw new IllegalArgumentException(
						"Invalid p-value for node ‘" +
						row.get(CyNetwork.NAME, String.class) +
						"’.");
			}
		}
		
		// stop if Cancel was clicked
		if (cancelled) { return; }
		
		taskMonitor.setStatusMessage("Setting up the workflow");
		
		// create an empty task iterator
		TaskIterator workflowTaskIterator = new TaskIterator();
		
		if (fitBum) {
			Task bumFittingTask = new BumFittingTask(
					network.getDefaultNodeTable().getColumn(
							pValueColumnName.getSelectedValue()),
					network.getTable(
							CyNetwork.class, CyNetwork.LOCAL_ATTRS).getRow(
									network.getSUID()),
					bumFittingStarts,
					bumServerHost, bumServerPort);
			workflowTaskIterator.append(bumFittingTask);
		}
		
		Task heinzTask;
		if (fitBum) {
			heinzTask = new HeinzTask(
					network,
					pValueColumnName.getSelectedValue(),
					resultColumnName,
					fdr.getValue(),
					null,
					null,
					heinzServerHost,
					heinzServerPort);
		} else {
			heinzTask = new HeinzTask(
					network,
					pValueColumnName.getSelectedValue(),
					resultColumnName,
					fdr.getValue(),
					lambda.getValue(),
					a.getValue(),
					heinzServerHost,
					heinzServerPort);
		}
		workflowTaskIterator.append(heinzTask);
		
		if (performGoEnrichment) {
			if (bridgeDbFile == null) {
				throw new IllegalArgumentException("No Bridge Derby database file selected");
			}
			Task goEnrichmentTask = new GoEnrichmentTask(
					network,
					bridgeDbFile.getPath(),
					idColumnSelector.getSelectedValue(),
					idTypeSelector.getSelectedValue(),
					resultColumnName,
					groupManager,
					groupFactory);
			workflowTaskIterator.append(goEnrichmentTask);
		}
		
		// append the tasks to the calling task iterator
		insertTasksAfterCurrentTask(workflowTaskIterator);
		
	}
}
