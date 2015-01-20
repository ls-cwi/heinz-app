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

import org.cytoscape.task.AbstractNetworkTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyColumn;
import org.cytoscape.group.CyGroupManager;
import org.cytoscape.group.CyGroupFactory;

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
public class GoEnrichmentTask extends AbstractNetworkTask {
	
	private enum GoNamespace {
		BIOLOGICAL_PROCESS,
		CELLULAR_COMPONENT,
		MOLECULAR_FUNCTION,
		EXTERNAL
	}
	
	private static class GoTerm {
		
		// TODO recurse from the top and collect all ancestors
		public String canonicalId;
		public GoNamespace namespace;
		public String name;
		public Set<GoTerm> childSet;
		
		public GoTerm() {
			this(null, null, null);
		}
		public GoTerm(String canonicalId, GoNamespace namespace, String name) {
			this.canonicalId = canonicalId;
			this.namespace = namespace;
			this.name = name;
			this.childSet = new HashSet<GoTerm>();
		}
		
	}
	
	public static final String BP_COLUMN_NAME = "‘biological process’ terms";
	public static final String CC_COLUMN_NAME = "‘cellular_component’ terms";
	public static final String MF_COLUMN_NAME = "‘molecular_function’ terms";
	
	public static final String TERM_NAME_COL = "term name";
	public static final String TERM_NAMESPACE_COL = "term namespace";
	public static final String TERM_PVAL_COL = "term p-value";
	
	private final String bridgeDbFileName;
	private final String idColumnName;
	private final String idType;
	private final String moduleColumnName;
	private final CyGroupManager groupManager;
	private final CyGroupFactory groupFactory;
	
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
    		CyNetwork network,
    		String bridgeDbFileName,
    		String idColumnName,
    		String idType,
    		String moduleColumnName,
    		CyGroupManager groupManager,
    		CyGroupFactory groupFactory) {
    	// set the `network' field
    	super(network);
    	// set the other parameters as fields
    	this.bridgeDbFileName = bridgeDbFileName;
    	this.idColumnName = idColumnName;
    	this.idType = idType;
    	this.moduleColumnName = moduleColumnName;
    	this.groupManager = groupManager;
    	this.groupFactory = groupFactory;
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
	 * Parse term information from a Gene Ontology ‘.obo’ file.
	 * 
	 * @param oboFile  the ontology file as an input stream
	 * @return a map from GO ids to objects holding other fields of the terms
	 * @throws IOException  if an error occurs reading and parsing the file
	 */
	private static Map<String, GoTerm> parseOboFile(InputStream oboFile)
			throws IOException {
		Map<String, GoTerm> goTermMap = new HashMap<String, GoTerm>();
		// loop over the lines of the file
		BufferedReader reader =
				new BufferedReader(new InputStreamReader(oboFile, "UTF-8"));
		// create an empty term object to hold the properties of the first stanza
		GoTerm term = new GoTerm();
		// keep track of the type of the current stanza
		String stanzaHeader = null;
		// loop over the lines in the OBO file
		String line;
		int lineNumber = 0;
		while ((line = reader.readLine()) != null) {
			lineNumber++;
			// if the line is a comment or a blank line
			if (line.startsWith("!") || line.equals("")) {
				// skip to the next line
				continue;
			// if this line marks the start of a new stanza
			} else if (line.startsWith("[") && line.endsWith("]")) {
				// TODO do this for the stanza terminated by end of file too
				// if this header terminates a preceding Term stanza
				if (stanzaHeader != null && stanzaHeader.equals("[Term]")) {
					// check if all required fields have been read for the previous term
					if (term.canonicalId == null)
						throw new IOException(
								"Encountered term without id in ontology file" +
								" (line " + lineNumber + ")");
					if (term.name == null)
						throw new IOException(
								"Encountered term without name in ontology file"+
								" (line " + lineNumber + ")");
					if (term.namespace == null)
						throw new IOException(
								"Encountered term without namespace in ontology file"+
								" (line " + lineNumber + ")");
					// create an object for the properties of the next term
					term = new GoTerm();
				}
				// replace the current stanza header
				stanzaHeader = line;
			// this line must be a tag-value pair, of the form
			// <tag>: <value> {<trailing modifiers>} ! <comment>
			} else {
				// if currently in a term stanza
				if (stanzaHeader != null && stanzaHeader.equals("[Term]")) {
					// strip off end-of-line comments, after an unescaped !
					line = line.replaceFirst("([^\\\\])!.*", "$1");
					// strip off trailing modifiers in (unescaped) braces
					line = line.replaceFirst("([^\\\\])\\{.*[^\\\\]\\} *$", "$1");
					// split up the tag-value pair at the first unescaped
					// colon followed by a space
					String fieldTag =
							line.replaceFirst("([^\\\\]): .*", "$1").trim();
					String fieldVal =
							line.replaceFirst(".*[^\\\\]: (.*)", "$1").trim();
					// check if the field is one of the required ones and handle it
					if (fieldTag.equals("id")) {
						if (term.canonicalId != null)
							throw new IOException(
									"Term " +
									term.canonicalId +
									" has multiple IDs.");
						term.canonicalId = fieldVal;
						// if an initialised term is already in the map for this id
						if (
								goTermMap.get(fieldVal) != null &&
								goTermMap.get(fieldVal).canonicalId != null)
							throw new IOException(
									"Multiple entries for " +
									fieldVal +
									" in Gene Ontology.");
						// add the object to the map for this id
						goTermMap.put(fieldVal, term);
					} else if (fieldTag.equals("alt_id")) {
						// if an initialised term is already in the map for this id
						if (
								goTermMap.get(fieldVal) != null &&
								goTermMap.get(fieldVal).canonicalId != null)
							throw new IOException(
									"Multiple entries for " +
									fieldVal +
									" in Gene Ontology.");
						// if the alt_id has been annotated as some term’s parent
						if (
								goTermMap.get(fieldVal) != null &&
								goTermMap.get(fieldVal).childSet.size() > 0)
							throw new IOException(
									"Non-canonical id " +
									fieldVal +
									" listed as parent of " +
									// the canonical id of one element of the child set
									goTermMap.get(fieldVal).childSet.toArray(new GoTerm[0])[0].canonicalId +
									" in Gene Ontology.");
						// add the object to the map for this id too
						goTermMap.put(fieldVal, term);
					} else if (fieldTag.equals("name")) {
						if (term.name != null)
							throw new IOException(
									"Term " +
									term.canonicalId +
									" has multiple names.");
						term.name = fieldVal;
					} else if (fieldTag.equals("namespace")) {
						if (term.namespace != null)
							throw new IOException(
									"Term " +
									term.canonicalId +
									" has multiple namespaces.");
						if (fieldVal.equals("biological_process")) {
							term.namespace = GoNamespace.BIOLOGICAL_PROCESS;
						} else if (fieldVal.equals("cellular_component")) {
							term.namespace = GoNamespace.CELLULAR_COMPONENT;
						} else if (fieldVal.equals("molecular_function")) {
							term.namespace = GoNamespace.MOLECULAR_FUNCTION;
						} else {
							throw new IOException(
									"Unexpected namespace in ontology file: " +
											fieldVal);
						}
					} else if (fieldTag.equals("is_a")) {
						// if the ‘parent’ term is not yet in the map
						if (!goTermMap.containsKey(fieldVal)) {
							// create an empty term object for it
							goTermMap.put(fieldVal, new GoTerm());
						}
						// add the current term to the child set of its parent
						goTermMap.get(fieldVal).childSet.add(term);
					}
				}
			}
		}
		
		return goTermMap;
	}
    
    /**
     * Create a new node table column, or overwrite an existing same-type one.
     * 
     * @param name  the name of the column to create or overwrite
     * @param type  the type of the column (may be List)
     * @param elementType  the type of the elements for a list column, or null
     * @throws IllegalArgumentException  if a column of another type is found
     */
    private void createNodeColumn(String name, Class<?> type, Class<?> elementType) {
    	CyTable table = network.getDefaultNodeTable();
    	// identify the column if it exists already
    	CyColumn column = table.getColumn(name);
    	// if there was no column with this name yet
    	if (column == null) {
    		// create the column
    		if (type == List.class) {
    			table.createListColumn(name, elementType, false);
    		} else {
    			table.createColumn(name, type, false);
    		}
    	// if the existing column is of the right type
    	} else if (
    			(type == List.class && column.getListElementType() == elementType) ||
    			(column.getType() == type)) {
    		// delete it and make a new, empty one
    		table.deleteColumn(name);
    		if (type == List.class) {
    			table.createListColumn(name, elementType, false);
    		} else {
    			table.createColumn(name, type, false);
    		}
    	// if the column is not of the right type
    	} else {
    		throw new IllegalArgumentException(
    				"Column ‘" +
    				name +
    				"’ is not of type ‘" +
    				(type == List.class ?
    						"List of " + elementType.getSimpleName() :
    						type.getSimpleName()) +
    				"’.");
    	}
    }
    
	/**
     * Find GO terms enriched in the network and add them as CyGroups.
     * 
	 * @throws IOException  if errors occur looking up the terms
     */
	@Override
	public void run(final TaskMonitor taskMonitor) throws IOException {
		
		// Give the task a title (shown in status monitor)
		taskMonitor.setTitle("GO Enrichment");
		
		taskMonitor.setStatusMessage(
				"Loading the Gene Ontology");
		
		ontologyMap = parseOboFile(getClass().getResourceAsStream("/data/go-basic.obo"));
		
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
		// TODO rename to goAnnotationMap or something
//    	Map<Xref, Set<Xref>> goTermMap;
//		try {
//			goTermMap = mapper.mapID(
//					sources,
//					DataSource.getByFullName("GeneOntology"));
//		} catch (IDMapperException e) {
//			throw new IOException("Could not query the loaded BridgeDB database");
//		}
		
		// create a column in the node table for terms in each namespace
		String[] termColNames = {
				BP_COLUMN_NAME,
				CC_COLUMN_NAME,
				MF_COLUMN_NAME };
		for (String colName : termColNames) {
			createNodeColumn(colName, List.class, String.class);
		}
		
		// create columns in the node table to represent properties of GO terms
		createNodeColumn(TERM_NAME_COL, String.class, null);
		createNodeColumn(TERM_NAMESPACE_COL, String.class, null);
		createNodeColumn(TERM_PVAL_COL, Double.class, null);
		
		// TODO handle pre-existing groups
		
		for (CyRow row : network.getDefaultNodeTable().getAllRows()) {
			// start with an empty list for each term column
			Set<String> bpTerms = new HashSet<String>();
			Set<String> ccTerms = new HashSet<String>();
			Set<String> mfTerms = new HashSet<String>();
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
				// look up the term’s details from the ontology
				GoTerm term = ontologyMap.get(termId);
				// add the id to the appropriate list
				if (term.namespace == GoNamespace.BIOLOGICAL_PROCESS) {
					bpTerms.add(term.canonicalId);
				} else if (term.namespace == GoNamespace.CELLULAR_COMPONENT) {
					ccTerms.add(term.canonicalId);
				} else if (term.namespace == GoNamespace.MOLECULAR_FUNCTION) {
					mfTerms.add(term.canonicalId);
				}
				// TODO add the term to a set if this node is part of the module
			}
			// set the values of the term columns for this row
			row.set(BP_COLUMN_NAME, new ArrayList<String>(bpTerms));
			row.set(CC_COLUMN_NAME, new ArrayList<String>(ccTerms));
			row.set(MF_COLUMN_NAME, new ArrayList<String>(mfTerms));
		}
		
		// TODO loop over the nodes again to add each node to the annotated groups and their parents
		
		// TODO test overrepresentation using the hypergeometric distribution
		
	}
	
}
