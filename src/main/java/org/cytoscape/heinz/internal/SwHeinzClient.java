package org.cytoscape.heinz.internal;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Locale;

import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyRow;


/**
 * Communicates with Heinz via a simple client-server protocol.
 */
public class SwHeinzClient extends AbstractSwClient implements HeinzClient {
	
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
	public void ping() throws IOException {
		super.ping();
	}
	
    /**
     * {@inheritDoc}
     */
	@Override
	public void sendNodeTable(
			final CyTable nodeTable,
			final String pValueColumnName)
					throws IOException {
		
		// find the name and type of the table’s primary key column
		String pkColumnName = nodeTable.getPrimaryKey().getName();
		Class<?> pkColumnType = nodeTable.getPrimaryKey().getType();
		
		// make an object to build up a byte array in a growing buffer
		ByteArrayOutputStream fileContents = new ByteArrayOutputStream();
		// an object to write text to the ByteArrayOutputStream
		PrintWriter writer = new PrintWriter(fileContents, true);
		
		// start the file with a commented header line
		writer.format("#node\tpval\n");
		for (CyRow node : nodeTable.getAllRows()) {
			// write the line for this node table row to the byte array
			writer.format(
					(Locale) null,
					"%s\t%g\n",
					node.get(pkColumnName, pkColumnType),
					node.get(pValueColumnName, Double.class));
		}
		
		// send the file to the server as the payload of a message
		new ClientMessage(
				ClientMessage.TYPE_INPUT_FILE,
				"-n",
				fileContents.toByteArray()).send(outputStream);
		receiveAck();
		
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
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void retrieveResults(CyTable nodeTable, String resultColumnName)
			throws IOException, IllegalArgumentException {
		// TODO Auto-generated method stub
		
	}
	
}