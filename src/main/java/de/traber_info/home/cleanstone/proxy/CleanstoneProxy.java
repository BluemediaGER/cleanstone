package de.traber_info.home.cleanstone.proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ServerSocket;
import java.net.Socket;

/**
 * Main server that listens for new client connections and starts the threads for further data handling.
 *
 * @author Oliver Traber
 */
public class CleanstoneProxy {

    /** SLF4J logger for usage in this class */
    private static final Logger LOG = LoggerFactory.getLogger(CleanstoneProxy.class.getName());

    /** Port on which cleanstone should be listening for client connections */
    private final int port;

    /**
     * Create a new instance of cleanstone's main proxy.
     * @param port Port on which cleanstone should be listening for client connections.
     */
    public CleanstoneProxy(int port) {
        this.port = port;
    }

    /**
     * Start the serverSocket to accept client connections.
     */
    public void listen() {
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            LOG.info("Listening on port {}", port);
            while (true) {
                Socket socket = serverSocket.accept();
                startThread(new Connection(socket));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Start a new thread to handle an incoming client connection.
     * @param connection Connection thread to handle the incoming client connection.
     */
    private void startThread(Connection connection) {
        Thread t = new Thread(connection);
        t.start();
    }

}
