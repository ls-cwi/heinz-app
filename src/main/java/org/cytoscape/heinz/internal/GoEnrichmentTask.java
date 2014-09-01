package org.cytoscape.heinz.internal;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

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
	
	private enum GoNamespace {
		BIOLOGICAL_PROCESS,
		CELLULAR_COMPONENT,
		MOLECULAR_FUNCTION,
		EXTERNAL
	}
	
	private static class GoTerm {
		
		public final GoNamespace namespace;
		public final String name;
		
		public GoTerm(GoNamespace namespace, String name) {
			this.namespace = namespace;
			this.name = name;
		}
		
	}
	
	private static Map<String, GoTerm> parseOboFile(InputStream oboFile)
			throws IOException {
		Map<String, GoTerm> goTermMap = new HashMap<String, GoTerm>();
		// loop over the lines of the file
		BufferedReader reader = 
				new BufferedReader(new InputStreamReader(oboFile, "UTF-8"));
		String line;
		while ((line = reader.readLine()) != null) {
			// when the start of a term entry is encountered
			if (line.equals("[Term]")) {
				// empty the variables for the fields
				String id = null;
				String name = null;
				GoNamespace namespace = null;
				// loop over the lines of the entry
				while ((line = reader.readLine()) != null) {
					// if this line marks the end of the entry
					if (line.equals("")) {
						// break out of the loop
						break;
					}
					// split the field into the name and the value
					String[] field = line.split(": ", 2);
					// check if the field is one of the required ones and handle it
					if (field[0].equals("id")) {
						id = field[1];
					} else if (field[0].equals("name")) {
						name = field[1];
					} else if (field[0].equals("namespace")) {
						if (field[1].equals("biological_process")) {
							namespace = GoNamespace.BIOLOGICAL_PROCESS;
						} else if (field[1].equals("cellular_component")) {
							namespace = GoNamespace.CELLULAR_COMPONENT;
						} else if (field[1].equals("molecular_function")) {
							namespace = GoNamespace.MOLECULAR_FUNCTION;
						} else {
							throw new IOException(
									"Unexpected namespace in ontology file: " + 
									field[1]);
						}
					}
				}
				// check if all required fields have been read for the entry
				if (id == null) {
					throw new IOException(
							"Encountered entry without id in ontology file");
				} else if (name == null) {
					throw new IOException(
							"Encountered entry without name in ontology file");
				} else if (namespace == null) {
					throw new IOException(
							"Encountered entry without namespace in ontology file");
				}
				// now the entry has been parsed, add it to the map
				goTermMap.put(id, new GoTerm(namespace, name));
			}
		}
		
		return goTermMap;
	}
	
	public final String BP_COLUMN_NAME = "`biological process' terms";
	public final String CC_COLUMN_NAME = "`cellular_component' terms";
	public final String MF_COLUMN_NAME = "`molecular_function' terms";
	
	private final String bridgeDbFileName;
	private final String idColumnName;
	private final String idType;
	private final String moduleColumnName;
	private Map<String, GoTerm> ontologyMap;
	
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
				"Loading the Gene Ontology");
		
		ontologyMap = parseOboFile(getClass().getResourceAsStream("/data/go.obo"));
		
		taskMonitor.setStatusMessage(
				"Loading the selected Bridge Derby database");
		
// TODO uncomment when BridgeDB is working to look up the terms in the database
//		// load the driver for loading the database
//    	try {
//    		// TODO
//    		Class.forName("org.bridgedb.rdb.IDMapperRdb");
//    		//Class.forName("org.bridgedb.webservice.bridgerest.BridgeRest"); 
//		} catch (ClassNotFoundException e) {
//			throw new RuntimeException("Database driver not found");
//		}
//    	
//
//    	// load the database, creating an IDMapper instance.
//    	IDMapper mapper = null;
//		try {
//			// TODO
//			mapper = BridgeDb.connect("idmapper-pgdb:" + bridgeDbFileName);
//			//mapper = BridgeDb.connect("idmapper-bridgerest:http://webservice.bridgedb.org/Mouse");
//			if (mapper != null) {
//				throw new IDMapperException();
//			}
//		} catch (IDMapperException e) {
//			throw new IOException(
//					e.getCause());
//					//TODO "Could not load database in " + bridgeDbFileName);
//		}
//    	
//		taskMonitor.setStatusMessage(
//				"Finding GO terms for the nodes");
//		
//		// look up the DataSource object for the gene ID type 
//		DataSource idDataSource = DataSource.getByFullName(idType);
//		
//    	// create a set of Xref instances for the identifiers to be looked up
//		Set<Xref> sources = new HashSet<Xref>();
//		for (String id : table.getColumn(idColumnName).getValues(String.class)) {
//			sources.add(new Xref(id, idDataSource));
//		}
//    	// query the GO terms for all of the ids
//    	Map<Xref, Set<Xref>> goTermMap;
//		try {
//			goTermMap = mapper.mapID(
//					sources,
//					DataSource.getByFullName("GeneOntology"));
//		} catch (IDMapperException e) {
//			throw new IOException("Could not query the loaded BridgeDB database");
//		}
		
		// create columns for terms of each namespace in the node table
		// TODO handle pre-existing columns
		table.createListColumn(BP_COLUMN_NAME, String.class, false);
		table.createListColumn(CC_COLUMN_NAME, String.class, false);
		table.createListColumn(MF_COLUMN_NAME, String.class, false);
		
		for (CyRow row : table.getAllRows()) {
			// start with an empty list for each term column
			List<String> bpTerms = new ArrayList<String>();
			List<String> ccTerms = new ArrayList<String>();
			List<String> mfTerms = new ArrayList<String>();
// TODO uncomment when BridgeDb is working to collect the terms for each node
//			// Make an Xref instance for the gene ID of this row
//			Xref idXref = new Xref(
//					row.get(idColumnName, String.class),
//					idDataSource);
//			// look up the GO terms for this ID
//			Set<Xref> rowTermXrefs = goTermMap.get(idXref);
//			// extract the actual GO id Strings and list them by namespace
//			for (Xref term : rowTermXrefs) {
//				String termId = term.getId()
			// TODO use the loop above instead when BridgeDb works
			for (String termId : row.getList("All terms", String.class)) {
				// check if the key exists in the loaded gene ontology
				if (!ontologyMap.containsKey(termId)) {
					throw new IOException("Unknown term " + termId + ".");
				}
				// look up the term’s namespace in the ontology
				GoNamespace termNamespace =
						ontologyMap.get(termId).namespace;
				// add the id to the appropriate list
				if (termNamespace == GoNamespace.BIOLOGICAL_PROCESS) {
					bpTerms.add(termId);
				} else if (termNamespace == GoNamespace.CELLULAR_COMPONENT) {
					ccTerms.add(termId);
				} else if (termNamespace == GoNamespace.MOLECULAR_FUNCTION) {
					mfTerms.add(termId);
				}
			}
			// set the values of the term columns for this row
			row.set(BP_COLUMN_NAME, bpTerms);
			row.set(CC_COLUMN_NAME, ccTerms);
			row.set(MF_COLUMN_NAME, mfTerms);
		}		
		
		// TODO create a group node for each term occurring in the module
		
		// TODO test overrepresentation using the hypergeometric distribution
		
	}
	
}
