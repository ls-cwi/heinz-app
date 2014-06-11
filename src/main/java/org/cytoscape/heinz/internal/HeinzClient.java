package org.cytoscape.heinz.internal;

import java.io.IOException;

import org.cytoscape.model.CyTable;

/**
 * Provides the HeinzTask with a way to run Heinz.
 */
public interface HeinzClient {
	
	/**
	 * Test if the (external) Heinz provider is present and ready.
	 * 
	 * @throws IOException  if Heinz cannot be reached for any reason
	 */
	public void ping() throws IOException;
	
	/**
	 * Set/write node attributes to be read by Heinz.
	 * 
	 * @param nodeTable  the node table in which the attributes can be found
	 * @param pValueColumnName  the name of the p-value column in nodeTable
	 * 
	 * @throws IOException  if the table cannot be written successfully
	 */
	public void sendNodeTable(CyTable nodeTable, String pValueColumnName)
			throws IOException;
	
	/**
	 * Set/write the edge table to be read by Heinz.
	 * 
	 * @param edgeTable  the edge table representing the network.
	 * 
	 * @throws IOException  if the table cannot be written successfully
	 */
	public void sendEdgeTable(CyTable edgeTable) throws IOException;
	
	/**
	 * Set/write the BUM mixture parameter for the Heinz run.
	 * 
	 * @param lambda  the mixture parameter of the BUM model
	 */
	public void sendLambda(double lambda) throws IOException;
	
	/**
	 * Set/write the BUM shape parameter for the Heinz run.
	 * 
	 * @param a  the shape parameter of the BUM model
	 */
	public void sendA(double a) throws IOException;
	
	/**
	 * Set/write the false discovery rate for the Heinz run.
	 * 
	 * @param fdr  the FDR parameter 
	 */
	public void sendFdr(double fdr);
	
	/**
	 * Run Heinz with the parameters set beforehand.
	 * 
	 * The parameters can be set using various other methods of this class.
	 * 
	 * @throws IOException  if Heinz does not terminate successfully
	 */
	public void runHeinz() throws IOException;
	
	/**
	 * Retrieve Heinz results and add a boolean column to the node table.
	 * 
	 * Heinz must have been run beforehand to generate the results. If the
	 * result column already exists and is of type boolean, it will be
	 * overwritten.
	 * 
	 * @param nodeTable  the node table, into which to add the column
	 * @param resultColumnName  name of the column to add
	 * 
	 * @throws IOException  if the results could not be retrieved
	 * @throws IllegalArgumentException  if the column is of the wrong type
	 * 
	 * @see #runHeinz()
	 */
	public void retrieveResults(CyTable nodeTable, String resultColumnName)
			throws IOException, IllegalArgumentException;
	
	/**
	 * End the connection (if applicable) after the run is over.
	 * 
	 * @throws IOException  if an I/O error occurs
	 */
	public void close() throws IOException;
	
}
