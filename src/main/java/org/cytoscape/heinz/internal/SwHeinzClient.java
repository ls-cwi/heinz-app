package org.cytoscape.heinz.internal;

import java.net.Socket;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.charset.Charset;
import java.io.IOException;
import java.io.EOFException;
import java.net.UnknownHostException;


import org.cytoscape.model.CyTable;

/**
 * Communicates with Heinz via a simple client-server protocol.
 */
public class SwHeinzClient implements HeinzClient {
	
	/**
	 * Represents a message from the client to the server wrapper.
	 */
	private static class ClientMessage {
		
		/**
		 * Ping request, to check if the server is responsive.
		 */
		public static final int TYPE_ALIVE = 0;
		/**
		 *  A simple (non-file) parameter for Heinz.
		 *  
		 *  This type of packet may have a payload, corresponding to a
		 *  command line argument.
		 */
		public static final int TYPE_PARAMETER = 10;
		/**
		 * An input file (parameter) for Heinz to read.
		 */
		public static final int TYPE_INPUT_FILE = 20;
		/**
		 * An output file (parameter) generation request.
		 * 
		 * This type of packet has no payload, the server will generate
		 * the parameter when receiving the packet.
		 */
		public static final int TYPE_OUTPUT_FILE = 30;
		/**
		 * A request to run Heinz.
		 */
		public static final int TYPE_RUN = 40;
		/**
		 * An output file retrieval request.
		 * 
		 * This packet requires a name identifying the file requested.
		 * Files associated with output file generation requests are
		 * identified by numbers starting at 0, and Heinz’s standard output
		 * and standard error streams by 254 and 255, respectively.
		 */
		public static final int TYPE_GET_OUTPUT = 50;
		
		private final int type;
		private final String name;
		private final byte[] payload;
		
		/**
		 * Construct a new client message
		 * 
		 * @param type  the message type, see ClientMessage class constants
		 * @param name  the name (command line flag) for parameters, or null
		 * @param payload  file contents, simple parameter argument, or null
		 * 
		 * @see #TYPE_ALIVE
		 * @see #TYPE_PARAMETER
		 * @see #TYPE_INPUT_FILE
		 * @see #TYPE_OUTPUT_FILE
		 * @see #TYPE_RUN
		 * @see #TYPE_GET_OUTPUT
		 */
		public ClientMessage(int type, String name, byte[] payload) {
			this.type = type;
			this.name = name;
			this.payload = payload;
		}
		
		/**
		 * Send this message to the server.
		 * 
		 * @param stream  an output stream connected to the server
		 */
		public void send(OutputStream stream) throws IOException {
			
			// wrap the OutputStream in a DataOutputStream, which implements
			// methods for conveniently writing primitive data types
			DataOutputStream dataStream = new DataOutputStream(stream);
			
			// write the type byte
			dataStream.writeByte(type);
			
			// if a name part is specified for the message
			if (name != null) {
				// encode the name string into a byte array
				byte[] nameByteArray = name.getBytes(Charset.forName("US-ASCII"));
				// send the length of the name as four bytes,
				// high byte first (big-endian, network byte order)
				dataStream.writeInt(nameByteArray.length);
				dataStream.write(nameByteArray);
			// if no name was specified
			} else {
				// indicate a name length of 0 (four bytes, NBO)
				dataStream.writeInt(0);
			}
			
			// if a payload was specified
			if (payload != null) {
				// send the length of the payload (four bytes, NBO)
				dataStream.writeInt(payload.length);
				dataStream.write(payload);
			// if no payload was specified 
			} else {
				// indicate a payload length of 0 (four bytes, NBO)
				dataStream.writeInt(0);
			}
			
			// actually send bytes if the stream happens to be buffered
			dataStream.flush();
			
		}
		
	}
	
	/**
	 * Represents a message from the server wrapper to the client.
	 */
	private static class ServerMessage {
		
		/**
		 * An acknowledgement that the server has processed a client message.
		 */
		public static final int TYPE_ACK = 8;
		/**
		 * An indication that the server could not process a client message.
		 */
		public static final int TYPE_NACK = 9;
		/**
		 * A package bearing an output stream as its payload.
		 */
		public static final int TYPE_OUTPUT = 59;
		
		private final int type;
		private final byte[] payload;
		
		/**
		 * Construct an object representing a (received) server message.
		 * 
		 * @param type  the message byte, see ServerMessage class constants
		 * @param payload  the payload of the message, if applicable, or null
		 * 
		 * @see #TYPE_ACK
		 * @see #TYPE_NACK
		 * @see #TYPE_OUTPUT
		 */
		private ServerMessage(int type, byte[] payload) {
			this.type = type;
			this.payload = payload;
		}
		
		/**
		 * Read a server message from an input stream.
		 * 
		 * @param stream  the stream to read from
		 * 
		 * @return  the server message read from the stream
		 * 
		 * @throws IOException  if any error occurs reading from the stream
		 * @throws EOFException  if the stream ends before the end of the message
		 */
		public static ServerMessage receive(InputStream stream)
				throws IOException {
			DataInputStream dataStream = new DataInputStream(stream);
			// read the type byte
			int type = dataStream.read();
			// read four bytes in big-endian byte order (network byte order)
			// and interpret them as an int, the length of the payload
			int payloadLength = dataStream.readInt();
			// create a byte array to store the payload
			byte[] payload = new byte[payloadLength];
			// try to fill the byte array and record the number of bytes
			// actually read
			int payloadBytesRead = dataStream.read(payload);
			if (payloadBytesRead != payloadLength) {
				throw new EOFException("Incomplete response from server.");
			}
			return new ServerMessage(type, payload);
		}
		
		/**
		 * Get the message type of this server message.
		 *  
		 * @return  the message type byte, see ServerMessage class constants
		 * 
		 * @see #TYPE_ACK
		 * @see #TYPE_NACK
		 * @see #TYPE_OUTPUT
		 */
		public int getType() {
			return type;
		}

		/**
		 * Get the payload of this server message.
		 * 
		 * @return the payload
		 */
		public byte[] getPayload() {
			return payload;
		}
		
	}
	
	private Socket socket;
	private InputStream inputStream;
	private OutputStream outputStream;
	
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
		
		socket = new Socket(host, port);
		inputStream = socket.getInputStream();
		outputStream = socket.getOutputStream();
		
		// test if the server can be reached
		ping();
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
		new ClientMessage(
				ClientMessage.TYPE_ALIVE, null, null).send(outputStream);
		ServerMessage response = ServerMessage.receive(inputStream);	
		if (!(
				response.getType() ==	ServerMessage.TYPE_ACK &&
				response.getPayload().length == 0)) {
			throw new IOException("Invalid response from server.");
		}
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