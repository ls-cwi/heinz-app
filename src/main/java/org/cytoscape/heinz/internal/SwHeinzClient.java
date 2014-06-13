package org.cytoscape.heinz.internal;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Locale;
import java.nio.charset.Charset;

import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyEdge;


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
		receiveAck();
		// ask the server to prepare for storing the Heinz output file
		new ClientMessage(
				ClientMessage.TYPE_OUTPUT_FILE, "-o", null).send(outputStream);
		receiveAck();
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
		
		// Check if the table has the node SUIDs as its primary key column
		if (
				nodeTable.getPrimaryKey().getName() != "SUID" ||
				nodeTable.getPrimaryKey().getType() != Long.class) {
			throw new IllegalArgumentException(
					"Primary key of node table (" +
					nodeTable.getPrimaryKey().getName() +
					") is not the SUID.");
		}
		
		// make an object to build up a byte array in a growing buffer
		ByteArrayOutputStream fileContents = new ByteArrayOutputStream();
		// an object to write text to the ByteArrayOutputStream
		PrintWriter writer = new PrintWriter(fileContents, true);
		
		// start the file with a commented header line
		writer.format("#node\tpval\n");
		// for each row in the node table
		for (CyRow nodeRow : nodeTable.getAllRows()) {
			// write the line for this node table row to the byte array
			writer.format(
					(Locale) null,
					"%d\t%g\n",
					nodeRow.get("SUID", Long.class),
					nodeRow.get(pValueColumnName, Double.class));
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
	public void sendEdgeTable(List<CyEdge> edgeList) throws IOException {
		
		// make an object to build up a byte array in a growing buffer
		ByteArrayOutputStream fileContents = new ByteArrayOutputStream();
		// an object to write text to the ByteArrayOutputStream
		PrintWriter writer = new PrintWriter(fileContents, true);
		
		// start the file with a commented header line
		writer.format("#source\ttarget\n");
		// for each edge
		for (CyEdge edge : edgeList) {
			// write the line for this node table row to the byte array
			writer.format(
					(Locale) null,
					"%d\t%d\n",
					edge.getSource().getSUID(),
					edge.getTarget().getSUID());
		}
		
		// send the file to the server as the payload of a message
		new ClientMessage(
				ClientMessage.TYPE_INPUT_FILE,
				"-e",
				fileContents.toByteArray()).send(outputStream);
		receiveAck();
		
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void sendLambda(double lambda) throws IOException {
		new ClientMessage(
				ClientMessage.TYPE_PARAMETER,
				"-lambda",
				((Double) lambda).toString().getBytes(
						Charset.forName("US-ASCII"))).send(outputStream);
		receiveAck();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void sendA(double a) throws IOException {
		new ClientMessage(
				ClientMessage.TYPE_PARAMETER,
				"-a",
				((Double) a).toString().getBytes(
						Charset.forName("US-ASCII"))).send(outputStream);
		receiveAck();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void sendFdr(double fdr) throws IOException {
		new ClientMessage(
				ClientMessage.TYPE_PARAMETER,
				"-FDR",
				((Double) fdr).toString().getBytes(
						Charset.forName("US-ASCII"))).send(outputStream);
		receiveAck();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void runHeinz() throws IOException {
		new ClientMessage(
				ClientMessage.TYPE_RUN,
				null,
				null).send(outputStream);
		receiveAck();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void retrieveResults(CyTable nodeTable, String resultColumnName)
			throws IOException, IllegalArgumentException {
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void close() throws IOException {
		super.close();
	}
	
}