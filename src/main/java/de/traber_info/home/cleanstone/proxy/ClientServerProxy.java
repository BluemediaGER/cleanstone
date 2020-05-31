package de.traber_info.home.cleanstone.proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Proxy to exchange data between the client and the backend server.
 *
 * @author Oliver Traber
 */
public class ClientServerProxy implements Runnable {

    /** SLF4J logger for usage in this class */
    private static final Logger LOG = LoggerFactory.getLogger(ClientServerProxy.class.getName());

    /** Socket from which data is read */
    private final Socket in;

    /** Socket to which data is written */
    private final Socket out;

    /**
     * Create a new instance of the ClientServerProxy.
     * @param in Socket from which data is read
     * @param out Socket to which data is written
     */
    public ClientServerProxy(Socket in, Socket out) {
        this.in = in;
        this.out = out;
    }

    /**
     * Start the thread that pushes the data back and forth between client and server.
     */
    @Override
    public void run() {
        try {
            InputStream inputStream = getInputStream();
            OutputStream outputStream = getOutputStream();

            if (inputStream == null || outputStream == null) {
                return;
            }

            byte[] reply = new byte[4096];
            int bytesRead;
            while (-1 != (bytesRead = inputStream.read(reply))) {
                outputStream.write(reply, 0, bytesRead);
            }
        } catch (Exception ex) {
            LOG.error("An unexpected error occurred...", ex);
        } finally {
            try {
                in.close();
            } catch (IOException ex2) {
                LOG.error("An unexpected error occurred...", ex2);
            }
        }
    }

    /**
     * Get the InputStream from which data is read.
     * @return InputStream from which data is read.
     */
    private InputStream getInputStream() {
        try {
            return in.getInputStream();
        } catch (IOException ex) {
            LOG.error("An unexpected error occurred...", ex);
        }
        return null;
    }

    /**
     * Get the OutputStream data is written to.
     * @return OutputStream data is written to.
     */
    private OutputStream getOutputStream() {
        try {
            return out.getOutputStream();
        } catch (IOException ex) {
            LOG.error("An unexpected error occurred...", ex);
        }
        return null;
    }

}
