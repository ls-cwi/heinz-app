package org.cytoscape.heinz.internal;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Locale;
import java.nio.charset.Charset;

import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyColumn;
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
		
		// Check if the node table has (node) SUIDs as its primary key column
		if (
				nodeTable.getPrimaryKey().getName() != "SUID" ||
				nodeTable.getPrimaryKey().getType() != Long.class) {
			throw new IllegalArgumentException(
					"Primary key of node table (" +
					nodeTable.getPrimaryKey().getName() +
					") is not the SUID.");
		}
		
		// retrieve the output file
		new ClientMessage(
				ClientMessage.TYPE_GET_OUTPUT,
				"0",
				null).send(outputStream);
		ServerMessage response = ServerMessage.receive(inputStream);
		if (response.getType() != ServerMessage.TYPE_OUTPUT) {
			throw new IOException("Invalid response from server.");
		}
		// decode the contents as text
		String outputFileContents = new String(
				response.getPayload(),
				Charset.forName("US-ASCII"));
		
		// identify and if necessary create the column to store the results in
		CyColumn resultColumn = nodeTable.getColumn(resultColumnName);
		// if there was no column with that name yet
		if (resultColumn == null) {
			// create the column
			nodeTable.createColumn(resultColumnName, Boolean.class, false);
		// check if the (existing) column is of the right type
		} else if (resultColumn.getType() != Boolean.class) {
			throw new IllegalArgumentException(
					"Selected output column is not of type Boolean.");
		}
		
		// wrap the string in an object that allows reading line by line
		BufferedReader reader = new BufferedReader(
				new StringReader(outputFileContents));
		String line;
		// for each line in the file
		while ((line = reader.readLine()) != null) {
			// if this is a comment line
			if (line.startsWith("#")) {
				// skip to the next line
				continue;
			}
			
			// Split on any (sequence of) whitespace characters
			String[] fields = line.split("\\s+");
			if (fields.length != 2) {
				throw new IOException(
						"File received from server does not have two columns");
			}
			
			// parse the first field (the ID) as a long integer
			long nodeSuid = Long.parseLong(fields[0]);
			// parse the second field as a floating-point number and test it:
			// it’s a score if the node is in the module or NaN if not
			boolean inModule = !Double.isNaN(Double.parseDouble(fields[1]));
			
			// set the result column item for this node
			nodeTable.getRow(nodeSuid).set(resultColumnName, inModule);
			
		}
		
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void close() throws IOException {
		super.close();
	}
	
}