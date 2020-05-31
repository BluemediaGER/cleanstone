package de.traber_info.home.cleanstone.proxy;

import de.traber_info.home.cleanstone.CleanStone;
import de.traber_info.home.cleanstone.model.object.Packet;
import de.traber_info.home.cleanstone.util.DatatypeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * Thread class for handling client connections.
 *
 * @author Oliver Traber
 */
public class Connection implements Runnable {

    /** SLF4J logger for usage in this class */
    private static final Logger LOG = LoggerFactory.getLogger(Connection.class.getName());

    /** DatatypeUtil that is used in this class. */
    private final DatatypeUtil datatypeUtil;

    /** Socket for communication with and from the client */
    private final Socket clientSocket;

    /** Socket for communication with and from the backend server */
    private Socket serverConnection = null;

    /**
     * Create a new instance to handle an incoming client connection.
     * @param clientSocket Socket for communication with and from the client.
     */
    public Connection(Socket clientSocket) {
        this.datatypeUtil = new DatatypeUtil();
        this.clientSocket = clientSocket;
    }

    /**
     * Main method to handle an incoming client connection. Tries to parse the handshake packet and route
     * the client traffic to the corresponding backend server.
     */
    @Override
    public void run() {
        LOG.info("Accepted new connection from {}:{}",
                clientSocket.getInetAddress().getHostAddress(),
                clientSocket.getPort()
        );

        try {
            // Read first packet
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            baos.write(buffer, 0 , clientSocket.getInputStream().read(buffer));
            byte[] result = baos.toByteArray();
            baos.close();

            Packet packet = new Packet(result);
            // Check if packet is an handshake packet
            if (packet.getPacketId() == 0) {
                // Parse protocol version
                int protocolVersion = datatypeUtil.readVarInt(packet.getUnreadData(), 0);
                String wantedServerAddress = datatypeUtil.readString(
                        packet.getUnreadData(),
                        datatypeUtil.getBytesRead()
                );
                LOG.info("Client {} connecting with protocol version {}. Wanted server: {}",
                        clientSocket.getInetAddress().getHostAddress(),
                        protocolVersion,
                        wantedServerAddress
                );

                // Check if the domain can be mapped to an backend server
                if (CleanStone.getBackendServerMappings().get(wantedServerAddress) != null) {

                    LOG.info("Backend server address for {} is {}",
                            wantedServerAddress,
                            CleanStone.getBackendServerMappings().get(wantedServerAddress).getBackendServerAddress()
                    );

                    // Create connection to the backend server
                    serverConnection = new Socket(
                            CleanStone.getBackendServerMappings().get(wantedServerAddress).getBackendServerAddress(),
                            CleanStone.getBackendServerMappings().get(wantedServerAddress).getBackendServerPort()
                    );

                    LOG.info("Starting proxy {}:{} <-> {}:{}...",
                            clientSocket.getInetAddress().getHostAddress(),
                            clientSocket.getPort(),
                            serverConnection.getInetAddress().getHostName(),
                            serverConnection.getPort()
                    );

                    // Start proxy threads to exchange data between the client and the backend server
                    new Thread(new ClientServerProxy(clientSocket, serverConnection)).start();
                    new Thread(new ClientServerProxy(serverConnection, clientSocket)).start();
                    // Write handshake packet unchanged to the backend server's OutputStream
                    serverConnection.getOutputStream().write(result);
                    new Thread(() -> {
                        while (true) {
                            if (clientSocket.isClosed()) {
                                LOG.info("Client socket ({}:{}) closed. Closing connection to backend server...",
                                        clientSocket.getInetAddress().getHostAddress(),
                                        clientSocket.getPort()
                                );
                                closeServerConnection();
                                break;
                            }

                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException ex) {
                                LOG.error("An unexpected error occurred...", ex);
                            }
                        }
                    }).start();
                }
            } else {
                LOG.error("Packet is not a handshake. Closing client socket.");
                clientSocket.close();
            }
        } catch (IOException ex) {
            LOG.error("An unexpected error occurred...", ex);
        }
    }

    private void closeServerConnection() {
        if (serverConnection != null && !serverConnection.isClosed()) {
            try {
                serverConnection.close();
            } catch (IOException ex) {
                LOG.error("An unexpected error occurred...", ex);
            }
        }
    }

}
