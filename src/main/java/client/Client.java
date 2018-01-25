package client;


import common.messages.KVMessage;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

public class Client extends Thread {

	private Logger logger = Logger.getRootLogger();
	private boolean running;
	
	private Socket clientSocket;
	private OutputStream output;
 	private InputStream input;
	private BufferedReader bufferedInputStream = null;
	private OutputStreamWriter outputStreamWriter = null;

 	private static final int TIMEOUT = 4*1000;
	
	private static final int BUFFER_SIZE = 1024;
	private static final int DROP_SIZE = 1024 * BUFFER_SIZE;
	
	
	public Client(String address, int port) 
			throws UnknownHostException, IOException {

		clientSocket = new Socket(address, port);
		setRunning(true);
		logger.info("Connection established");
        output = clientSocket.getOutputStream();
        input = clientSocket.getInputStream();

		bufferedInputStream = new BufferedReader(new InputStreamReader(input));
		outputStreamWriter = new OutputStreamWriter(clientSocket.getOutputStream());
	}
	
	public synchronized void closeConnection() {
		logger.info("try to close connection ...");
		
		try {
			tearDownConnection();
		} catch (IOException ioe) {
			logger.error("Unable to close connection!");
		}
	}
	
	private void tearDownConnection() throws IOException {
		setRunning(false);
		logger.info("tearing down the connection ...");
		if (clientSocket != null) {
			//input.close();
			//output.close();
			clientSocket.close();
			clientSocket = null;
			logger.info("connection closed!");
		}
	}

	public String getMessage() {
		String response = null;
		if (isRunning()) {
			try {
				response = receiveMessage();
			} catch (IOException ioe) {
				if(isRunning()) {
//					System.out.println("Error:> "+ioe.getMessage());
					logger.error("Connection lost!");
						closeConnection();
				}
			}
		}
		return response;
	}
	
	public boolean isRunning() {
		return running;
	}
	
	public void setRunning(boolean run) {
		running = run;
	}
	
	/**
	 * Method sends a TextMessage using this socket.
	 * @param msg the message that is to be sent.
	 * @throws IOException some I/O error regarding the output stream
	 */
	public void sendMessage(String msg) throws IOException {
		outputStreamWriter.write(msg + "\r\n");
		logger.info("Send message:\t '" + msg + "'");
    }
	
	
	private String receiveMessage() throws IOException {
		String msg = null;

		long startTime = System.currentTimeMillis();

		while(System.currentTimeMillis() - startTime < TIMEOUT) {
            if ( bufferedInputStream.ready()) {
                msg = bufferedInputStream.readLine();
                logger.info("Receive message:\t '" + msg + "'");
                return msg;
            }
        }
		return null;
    }
 	
}
