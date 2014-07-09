package org.cytoscape.heinz.internal;

import java.io.IOException;

public interface BumFitterClient {
	
	/**
	 * Set the list of p-values to fit a BUM model to.
	 * 
	 * @param pvalues  an array containing the p-values
	 * 
	 * @throws IOException  if an I/O error occurs
	 */
	public void sendPValues(double[] pvalues)
			throws IOException, IllegalStateException;
	
	/**
	 * Make sure plots will be generated when <code>run()</code> is called.
	 * 
	 * @throws IOException  if an I/O error occurs
	 */
	public void enablePlotting() throws IOException;
	
	/**
	 * Fit a BUM model using the options set beforehand.
	 * 
	 * @throws IOException  if the model fitting was unsuccessful
	 * 
	 * @see #sendPValues(double[])
	 * @see #enablePlotting()
	 */
	public void run() throws IOException;
	
	/**
	 * Get the fitted value for the mixture parameter.
	 * 
	 * @return  the fitted BUM model’s mixture parameter (λ)
	 * 
	 * @throws IOException  if an I/O error occurs or results are not present
	 * 
	 * @see #run()
	 */
	public double getLambda() throws IOException;
	
	/**
	 * Get the fitted value for the shape parameter.
	 * 
	 * @return  the fitted BUM model’s shape parameter (a)
	 * 
	 * @throws IOException  if an I/O error occurs or results are not present
	 * 
	 * @see #run()
	 */
	public double getA() throws IOException;
	
	/**
	 * Get the plots generated while fitting in PNG format.
	 * 
	 * @return  the contents of the PNG file as a byte array  
	 * 
	 * @throws IOException  if an I/O error occurs or no plots have been made
	 * 
	 * @see #run()
	 * @see #enablePlotting()
	 */
	public byte[] getPlotPng() throws IOException;
	
	/**
	 * End the connection (if applicable) when it is no longer needed.
	 * 
	 * @throws IOException  if an I/O error occurs
	 */
	public void close() throws IOException;
	
}
