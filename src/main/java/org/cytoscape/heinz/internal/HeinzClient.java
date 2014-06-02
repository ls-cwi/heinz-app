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
	 * @throws IOException if Heinz cannot be reached for any reason.
	 */
	public void ping() throws IOException;
	
	/**
	 * Set/write node attributes to be read by Heinz.
	 * 
	 * @param nodeTable - the node table in which the attributes can be found.
	 * @param pValueColumnName - the name of the column in nodeTable.
	 * 
	 * @throws IOException if the table cannot be written.
	 */
	public void sendNodeTable(CyTable nodeTable, String pValueColumnName)
			throws IOException;
	
	/**
	 * Set/write the edge table to be read by Heinz.
	 * 
	 * @param edgeTable - the edge table representing the network.
	 * 
	 * @throws IOException if the table cannot be written.
	 */
	public void sendEdgeTable(CyTable edgeTable) throws IOException;
	
	/**
	 * Set/write the BUM mixture parameter for the Heinz run.
	 * 
	 * @param lambda - the mixture parameter of the BUM model
	 */
	public void sendLambda(double lambda) throws IOException;
	
	/**
	 * Set/write the BUM shape parameter for the Heinz run.
	 * 
	 * @param a - the shape parameter of the BUM model
	 */
	public void sendA(double a) throws IOException;
	
	/**
	 * Set write the false discovery rate for the Heinz run.
	 * 
	 * @param fdr - the FDR parameter 
	 */
	public void sendFdr(double fdr);
	
	/**
	 * Run Heinz with the parameters set beforehand.
	 * 
	 * @throws IOException if Heinz does not finish successfully.
	 */
	public void runHeinz() throws IOException;
	
	// TODO decide what the getResults() method should return
	
}
