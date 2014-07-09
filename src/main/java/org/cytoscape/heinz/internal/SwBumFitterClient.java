package org.cytoscape.heinz.internal;


import java.io.ByteArrayOutputStream;
import java.io.Writer;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.BufferedReader;
import java.nio.charset.Charset;
import java.io.IOException;
import java.net.UnknownHostException;



/**
 * Communicates with the fitBum R script via a simple client-server protocol.
 */
public class SwBumFitterClient extends AbstractSwClient implements BumFitterClient {
	
	/**
	 * local copy of the output file contents after the run.
	 */
	private String outputFile = null;
	
	/**
	 * Initialise a connection to a Heinz server.
	 * 
	 * @param host  the host name of the server
	 * @param port  the port number to connect to
	 * 
	 * @throws IOException  if a connection to a compatible Heinz server cannot be made
	 * @throws UnknownHostException  if the server’s IP address could not be determined
	 */
	public SwBumFitterClient(String host, int port) throws
			IOException, UnknownHostException {
		
		// open a connection to the server,
		// and set outputStream an inputStream
		super(host, port);
		
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void sendPValues(double[] pvalues) throws IOException {
		
		// make an object to build up a byte array in a growing buffer
		ByteArrayOutputStream fileContents = new ByteArrayOutputStream();
		// make an object to write bytes encoded as ASCII to that
		Writer writer = new OutputStreamWriter(
				fileContents,
				Charset.forName("US-ASCII"));
		// and make an object to write to that using print methods
		PrintWriter printWriter = new PrintWriter(writer, true);

		// for each p-value
		for (double p : pvalues) {
			// write a line to the byte array
			printWriter.format("%.15g\n", p);
		}
		
		// send the file to the server as the payload of a message
		new ClientMessage(
				ClientMessage.TYPE_INPUT_FILE,
				"-i",
				fileContents.toByteArray()).send(outputStream);
		receiveAck();
		
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void sendStarts(int starts) throws IOException {
		new ClientMessage(
				ClientMessage.TYPE_PARAMETER,
				"-s",
				((Integer) starts).toString().getBytes(
						Charset.forName("US-ASCII"))).send(outputStream);
		receiveAck();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void enablePlotting() throws IOException {
		// ask the server to request plots and to prepare for storing them
		new ClientMessage(
				ClientMessage.TYPE_OUTPUT_FILE, "-p", null).send(outputStream);
		receiveAck();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void run() throws IOException {
		
		// instruct the server to run the BUM fitting script
		new ClientMessage(
				ClientMessage.TYPE_RUN,
				null,
				null).send(outputStream);
		receiveAck();
		
		// download the output file generated so it can be parsed locally
		new ClientMessage(
				ClientMessage.TYPE_GET_OUTPUT,
				"254",
				null);
		ServerMessage response = ServerMessage.receive(inputStream);
		if (response.getType() != ServerMessage.TYPE_OUTPUT) {
			throw new IOException("No output file received from server.");
		}
		
		// parse the contents of the file as plain text and set the field
		outputFile = new String(
				response.getPayload(),
				Charset.forName("US-ASCII"));
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public double getLambda() throws IOException {
		
		if (outputFile == null) {
			throw new IOException("No BUM model fit found.");
		}
		
		Double lambda = null;
		
		// wrap the string in an object that allows reading line by line
		BufferedReader reader = new BufferedReader(
				new StringReader(outputFile));
		String line;
		// for each line in the file
		while ((line = reader.readLine()) != null) {
			if (line.startsWith("Mixture parameter (lambda):")) {
				if (lambda != null) {
					throw new IOException(
							"Multiple lambda lines found in output file");
				}
				lambda = Double.parseDouble(
						line.replaceFirst(
								"^Mixture parameter \\(lambda\\):\\s*",
								""));
			}
		}
		if (lambda == null) {
			throw new IOException("No value for lambda found in output file");
		}
		return ((double) lambda);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public double getA() throws IOException {
		
		if (outputFile == null) {
			throw new IOException("No BUM model fit found.");
		}
		
		Double a = null;
		
		// wrap the string in an object that allows reading line by line
		BufferedReader reader = new BufferedReader(
				new StringReader(outputFile));
		String line;
		// for each line in the file
		while ((line = reader.readLine()) != null) {
			if (line.startsWith("shape parameter (a):")) {
				if (a != null) {
					throw new IOException(
							"Multiple a lines found in output file");
				}
				a = Double.parseDouble(
						line.replaceFirst(
								"^shape parameter \\(a\\):\\s*",
								""));
			}
		}
		
		if (a == null) {
			throw new IOException("No value for a found in output file");
		}
		return ((double) a);

	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public byte[] getPlotPng() throws IOException {
		// retrieve the output file
		new ClientMessage(
				ClientMessage.TYPE_GET_OUTPUT,
				"0",
				null).send(outputStream);
		ServerMessage response = ServerMessage.receive(inputStream);
		if (response.getType() != ServerMessage.TYPE_OUTPUT) {
			throw new IOException("No plot file received from server.");
		}
		return response.getPayload();
	}
	
    /**
	 * {@inheritDoc}
	 */
	@Override
	public void close() throws IOException {
		super.close();
	}
	
}