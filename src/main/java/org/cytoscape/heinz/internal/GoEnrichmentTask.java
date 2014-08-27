package org.cytoscape.heinz.internal;


import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.io.IOException;

import org.cytoscape.task.AbstractTableTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyRow;
import org.bridgedb.BridgeDb;
import org.bridgedb.DataSource;
import org.bridgedb.bio.BioDataSource;
import org.bridgedb.Xref;
import org.bridgedb.IDMapper;
import org.bridgedb.IDMapperException;


/**
 * Task to gather and  GO terms overrepresented in a given subnetwork.
 */
@SuppressWarnings("deprecation")
public class GoEnrichmentTask extends AbstractTableTask {
	
	private final String bridgeDbFileName;
	private final String idColumnName;
	private final String idType;
	private final String moduleColumnName;
	
	/**
 	 * Initialise the task, obtaining required parameters.
 	 * 
	 * @param nodeTable  the node table of the full network
	 * @param bridgeDbFilename  filename of the BridgeDerby database to use
	 * @param idColumnName  name of the node table column containing gene IDs
	 * @param idType  full name (according to BridgeDB) of the IDs
	 */
    public GoEnrichmentTask(
    		CyTable nodeTable,
    		String bridgeDbFileName,
    		String idColumnName,
    		String idType,
    		String moduleColumnName) {
    	// set the `table' field
    	super(nodeTable);
    	// set the other parameters as fields
    	this.bridgeDbFileName = bridgeDbFileName;
    	this.idColumnName = idColumnName;
    	this.idType = idType;
    	this.moduleColumnName = moduleColumnName;
    }
    
    public static String[] getSupportedIdTypes() {
    	BioDataSource.init();
    	Set<DataSource> availableDbs = 
    			DataSource.getFilteredSet(true, false, null);
    	List<String> availableDbNames = new ArrayList<String>();
    	for (DataSource db : availableDbs) {
    		availableDbNames.add(db.getFullName());
    	}
    	return availableDbNames.toArray(new String[0]);
    }

	/**
     * Find GO terms enriched in the network and add them as group nodes.
     * 
	 * @throws IOException  if errors in 
     */
	@Override
	public void run(final TaskMonitor taskMonitor) throws IOException {
		
		// Give the task a title (shown in status monitor)
		taskMonitor.setTitle("GO Enrichment");
				
		taskMonitor.setStatusMessage(
				"Loading the Bridge Derby database");
		
		// TODO fix the dependencies for Bridge Derby databases to make this work
		if (false) {
		// Load the driver for loading the database
    	try {
    		// TODO
    		Class.forName("org.bridgedb.rdb.IDMapperRdb");
    		//Class.forName("org.bridgedb.webservice.bridgerest.BridgeRest"); 
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("Database driver not found");
		}
    	

    	// Load the database, creating an IDMapper instance.
    	IDMapper mapper = null;
		try {
			// TODO
			mapper = BridgeDb.connect("idmapper-pgdb:" + bridgeDbFileName);
			//mapper = BridgeDb.connect("idmapper-bridgerest:http://webservice.bridgedb.org/Mouse");
			if (mapper != null) {
				throw new IDMapperException();
			}
		} catch (IDMapperException e) {
			throw new IOException(
					e.getCause());
					//TODO "Could not load database in " + bridgeDbFileName);
		}
    	
		taskMonitor.setStatusMessage(
				"Finding GO terms for the nodes");
		
		// Look up the DataSource object for the gene ID type 
		DataSource idDataSource = DataSource.getByFullName(idType);
		
    	// Create a set of Xref instances for the identifiers to be looked up
		Set<Xref> sources = new HashSet<Xref>();
		for (String id : table.getColumn(idColumnName).getValues(String.class)) {
			sources.add(new Xref(id, idDataSource));
		}
    	// look up the GO terms for each of these Xref instances
    	Map<Xref, Set<Xref>> goTermMap;
		try {
			goTermMap = mapper.mapID(
					sources,
					DataSource.getByFullName("GeneOntology"));
		} catch (IDMapperException e) {
			throw new IOException("Could not query the loaded BridgeDB database");
		}
		
		// TODO separate the GO Terms into cellular components, biological
		// processes and molecular functions
		
		table.createListColumn("All terms", String.class, false);
		for (CyRow row : table.getAllRows()) {
			// Make an Xref instance for the gene ID of this row
			Xref idXref = new Xref(
					row.get(idColumnName, String.class),
					idDataSource);
			// Look up the GO terms for this ID
			Set<Xref> rowTermXrefs = goTermMap.get(idXref);
			// extract the actual String
			List<String> rowTermStrings =
					new ArrayList<String>(rowTermXrefs.size());
			for (Xref term : rowTermXrefs) {
				rowTermStrings.add(term.getId());
			}
			row.set("all terms", rowTermStrings);
		}
		}
		
		// TODO create a group node for each term occurring in the module
		
		// TODO test overrepresentation using the hypergeometric distribution
		
	}
	
}
