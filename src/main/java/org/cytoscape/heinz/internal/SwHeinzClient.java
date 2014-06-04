package org.cytoscape.heinz.internal;

import java.io.IOException;
import java.net.UnknownHostException;

import org.cytoscape.model.CyTable;

/**
 * Communicates with Heinz via a simple client-server protocol.
 */
public class SwHeinzClient extends SwClient implements HeinzClient {
	
	/**
	 * Initialise a connection to a Heinz server.
	 * 
	 * @param host  the host name of the server
	 * @param port  the port number to connect to
	 * 
	 * @throws IOException  if a connection to a compatible Heinz server cannot be made
	 * @throws UnknownHostException  if the server’s IP address could not be determined
	 */
	public SwHeinzClient(String host, int port) throws
			IOException, UnknownHostException {
		
		// open a connection to the server
		super(host, port);
		
		// enable pre-processing, as will likely be the default in the future
		new ClientMessage(
				ClientMessage.TYPE_PARAMETER, "-p", null).send(outputStream);
		// ask the server to prepare for storing the Heinz output file
		new ClientMessage(
				ClientMessage.TYPE_OUTPUT_FILE, "-o", null).send(outputStream);
	}
	
    /**
     * {@inheritDoc}
     */
	@Override
	public void sendNodeTable(CyTable nodeTable, String pValueColumnName)
			throws IOException {
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void sendEdgeTable(CyTable edgeTable) throws IOException {
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void sendLambda(double lambda) throws IOException {
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void sendA(double a) throws IOException {
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void sendFdr(double fdr) {
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void runHeinz() throws IOException {
		// TODO Auto-generated method stub
		
	}
	
}