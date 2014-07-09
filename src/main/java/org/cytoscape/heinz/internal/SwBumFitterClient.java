package org.cytoscape.heinz.internal;


import java.io.IOException;
import java.net.UnknownHostException;


/**
 * Communicates with the fitBum R script via a simple client-server protocol.
 */
public class SwBumFitterClient extends AbstractSwClient implements BumFitterClient {
	
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
		// TODO Auto-generated method stub
		
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void sendStarts(int starts) throws IOException {
		// TODO Auto-generated method stub
		
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void enablePlotting() throws IOException {
		// TODO Auto-generated method stub
		
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void run() throws IOException {
		// TODO Auto-generated method stub
		
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public double getLambda() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public double getA() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public byte[] getPlotPng() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}
	
    /**
	 * {@inheritDoc}
	 */
	@Override
	public void close() throws IOException {
		super.close();
	}
	
}