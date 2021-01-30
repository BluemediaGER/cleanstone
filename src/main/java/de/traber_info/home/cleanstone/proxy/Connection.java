package de.traber_info.home.cleanstone.proxy;

import de.traber_info.home.cleanstone.CleanStone;
import de.traber_info.home.cleanstone.model.config.ConfigFile;
import de.traber_info.home.cleanstone.model.object.Packet;
import de.traber_info.home.cleanstone.util.ConfigUtil;
import de.traber_info.home.cleanstone.util.DatatypeUtil;
import de.traber_info.home.cleanstone.util.ProxyProtoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.regex.Pattern;

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

    /** Regex to remove Forge Modloader address appendix */
    private final static Pattern fmlPattern = Pattern.compile("\u0000FML.*\u0000");

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
        ConfigFile.ProxyProtocolSettings proxyProtocolSettings = ConfigUtil.getConfig().getProxyProtocolSettings();

        try {
            // Read first packet
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            baos.write(buffer, 0 , clientSocket.getInputStream().read(buffer));
            byte[] result = baos.toByteArray();
            baos.close();

            boolean hasProxyProtocolHeader = false;
            String clientIP;
            Packet packet;
            if (ProxyProtoUtil.hasProxyProtocolHeader(result)) {
                if (!proxyProtocolSettings.isEnabled() || !proxyProtocolSettings.passThroughEnabled()) {
                    clientSocket.close();
                    LOG.warn("Aborted connection from {}:{}. " +
                            "The received packet contains a PROXY protocol v2 header, " +
                            "but PROXY protocol pass through is disabled.",
                            clientSocket.getInetAddress().getHostAddress(),
                            clientSocket.getPort()
                    );
                    return;
                }
                hasProxyProtocolHeader = true;
                ProxyProtoUtil.ProxyProtoHeader proxyHeader = ProxyProtoUtil.decode(result, true);
                clientIP = proxyHeader.sourceAddress.getHostAddress();
                LOG.info("Accepted new connection from {}:{} via proxy {}",
                        clientIP,
                        proxyHeader.sourcePort,
                        clientSocket.getInetAddress().getHostAddress()
                );
                int headerLength = ProxyProtoUtil.getHeaderLength(result);
                int payloadLength = result.length - headerLength;
                byte[] mcPacket = new byte[payloadLength];
                System.arraycopy(result, headerLength, mcPacket, 0, payloadLength);
                packet = new Packet(mcPacket);
            } else {
                clientIP = clientSocket.getInetAddress().getHostAddress();
                LOG.info("Accepted new connection from {}:{}",
                        clientIP,
                        clientSocket.getPort()
                );
                packet = new Packet(result);
            }

            // Check if packet is an handshake packet
            if (packet.getPacketId() == 0) {
                // Parse protocol version
                int protocolVersion = datatypeUtil.readVarInt(packet.getUnreadData(), 0);

                // Parse wantedServerAddress
                String wantedServerAddress = datatypeUtil.readString(
                        packet.getUnreadData(),
                        datatypeUtil.getBytesRead()
                );
                // Remove FML appendix from wantedServerAddress if the connecting client is using Minecraft Forge
                wantedServerAddress = fmlPattern.matcher(wantedServerAddress).replaceAll("");

                LOG.info("Client {} connecting with protocol version {}. Wanted server: {}",
                        clientIP,
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

                    if (hasProxyProtocolHeader) {
                        LOG.info("Starting proxy {}:{} <-> {}:{} on behalf of client {}...",
                                clientSocket.getInetAddress().getHostAddress(),
                                clientSocket.getPort(),
                                serverConnection.getInetAddress().getHostAddress(),
                                serverConnection.getPort(),
                                clientIP
                        );
                    } else {
                        LOG.info("Starting proxy {}:{} <-> {}:{}...",
                                clientSocket.getInetAddress().getHostAddress(),
                                clientSocket.getPort(),
                                serverConnection.getInetAddress().getHostAddress(),
                                serverConnection.getPort()
                        );
                    }

                    // Start proxy threads to exchange data between the client and the backend server
                    new Thread(new ClientServerProxy(clientSocket, serverConnection)).start();
                    new Thread(new ClientServerProxy(serverConnection, clientSocket)).start();

                    if (proxyProtocolSettings.isEnabled() && !hasProxyProtocolHeader) {
                        // Add PROXY protocol header if PROXY protocol support is enabled
                        // and the packet doesn't contain a header yet.
                        byte[] header = ProxyProtoUtil.encode(
                                ProxyProtoUtil.TransportFam.TCP,
                                clientSocket.getInetAddress(),
                                clientSocket.getPort(),
                                clientSocket.getLocalAddress(),
                                clientSocket.getLocalPort()
                        );
                        byte[] combined = new byte[header.length + result.length];
                        System.arraycopy(header, 0, combined, 0, header.length);
                        System.arraycopy(result, 0, combined, header.length, result.length);
                        serverConnection.getOutputStream().write(combined);
                    } else {
                        // Write handshake packet unchanged to the backend server's OutputStream
                        serverConnection.getOutputStream().write(result);
                    }

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
                            if (serverConnection.isClosed()) {
                                LOG.info("Server connection for client ({}:{}) closed. Closing connection to client...",
                                        clientSocket.getInetAddress().getHostAddress(),
                                        clientSocket.getPort()
                                );
                                try {
                                    clientSocket.close();
                                } catch (IOException e) {
                                    // Do nothing
                                }
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
                LOG.error("Packet does not contain a handshake. Closing client socket.");
                clientSocket.close();
            }
        } catch (IOException ex) {
            LOG.error("An unexpected error occurred...", ex);
            try {
                clientSocket.close();
            } catch (IOException ex1) {
                // Do nothing
            }
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
