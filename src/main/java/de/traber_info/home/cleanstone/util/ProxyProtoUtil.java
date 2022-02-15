package de.traber_info.home.cleanstone.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Utility class to handle PROXY protocol v2 headers.
 *
 * @author Oliver Traber
 */
public class ProxyProtoUtil {

    /** PROXY protocol v2 signature bytes */
    private static final byte[] sigBytes = { 0x0D, 0x0A, 0x0D, 0x0A, 0x00, 0x0D, 0x0A, 0x51, 0x55, 0x49, 0x54, 0x0A };

    /** PROXY protocol v2 version indicator */
    private static final byte v2 = 0x21;

    /** Value offsets for default header fields */
    private static class Fields {
        public static final byte sig = 0x00;
        public static final byte ver_cmd = 0x0c;
        public static final byte family = 0x0d;
        public static final byte len = 0x0e;
        public static final byte addr = 0x10;
    }

    /** Value offsets for IPv4 connections */
    private static class Ipv4Addr {
        public static final byte src_addr = 0x00;
        public static final byte dst_addr = 0x04;
        public static final byte src_port = 0x08;
        public static final byte dst_port = 0x0a;
        public static final byte len = 0x0c;
    }

    /** Value offsets for IPv6 connections */
    private static class Ipv6Addr {
        public static final byte src_addr = 0x00;
        public static final byte dst_addr = 0x10;
        public static final byte src_port = 0x20;
        public static final byte dst_port = 0x22;
        public static final byte len = 0x24;
    }

    /** Network protocol type indicators */
    private static class Families {
        public static final byte AF_INET = 0x10;
        public static final byte AF_INET6 = 0x20;
    }

    /** Transport protocol type indicators */
    private static class Proto {
        public static final byte STREAM = 0x01;
        public static final byte DGRAM = 0x02;
    }

    /** Enum of supported transport protocols */
    public enum TransportFam {
        TCP, UDP
    }

    /** Enum of supported network protocols */
    public enum NetFam {
        IPv4, IPv6
    }

    /**
     * Class to represent an PROXY protocol v2 header.
     */
    public static class ProxyProtoHeader {
        /** Transport protocol family used to initiate the connection */
        public TransportFam transportFam;
        /** Network protocol family used to initiate the connection */
        public NetFam networkFamily;
        /** IP address of the client */
        public InetAddress sourceAddress;
        /** IP address of the receiving interface */
        public InetAddress destinationAddress;
        /** Port the connection is coming from */
        public int sourcePort;
        /** Port the connection is received on the interface address */
        public int destinationPort;
    }

    /**
     * Check if an buffer contains an PROXY protocol v2 header.
     * @param buffer byte[] that should be checked for PROXY protocol headers.
     * @return true if the given byte[] contains an PROXY protocol signature, otherwise false.
     */
    public static boolean hasProxyProtocolHeader(byte[] buffer) {
        if (buffer.length >= 16) {
            byte[] readSigBytes = new byte[sigBytes.length];
            System.arraycopy(buffer, 0, readSigBytes, 0, sigBytes.length);
            return Arrays.equals(sigBytes, readSigBytes);
        }
        return false;
    }

    /**
     * Get the length of an PROXY protocol v2 header in the given byte array.
     * @param buffer Byte array to get the headers length from.
     * @return Length of the header in full bytes.
     */
    public static int getHeaderLength(byte[] buffer) {
        byte[] headerPayloadLengthData = new byte[2];
        System.arraycopy(buffer, Fields.len, headerPayloadLengthData,0, 2);
        return decodeUInt8(headerPayloadLengthData, (byte) 0x00) + 16;
    }

    /**
     * Decode an PROXY protocol v2 header to an {@link ProxyProtoHeader} object.
     * @param buffer Byte array the header should be read from.
     * @param validate Set true to validate the header.
     *                 If validation errors occur, an {@link UnsupportedOperationException} will be thrown.
     * @return {@link ProxyProtoHeader} object containing the metadata parsed from the raw header.
     * @throws UnknownHostException Thrown if an IP address can't be converted to na {@link InetAddress}.
     * @throws UnsupportedOperationException Thrown if an validation check fails. Only applies if validate is true.
     */
    public static ProxyProtoHeader decode(byte[] buffer, boolean validate) throws UnknownHostException {
        byte enc_family = buffer[Fields.family];
        byte family = (byte) (enc_family & 0xf0);
        byte proto = (byte) (enc_family & 0x0f);

        if (validate) {
            if (!hasProxyProtocolHeader(buffer))
                throw new UnsupportedOperationException("proxy protocol: invalid signature received");
            if (buffer[Fields.ver_cmd] != v2)
                throw new UnsupportedOperationException("proxy protocol: invalid version or cmd received");
            if (family != Families.AF_INET && family != Families.AF_INET6)
                throw new UnsupportedOperationException("proxy protocol: unsupported family received");
            if (proto != Proto.STREAM && proto != Proto.DGRAM)
                throw new UnsupportedOperationException("proxy protocol: unsupported protocol received");
        }

        ProxyProtoHeader packet = new ProxyProtoHeader();
        packet.transportFam = (proto == Proto.STREAM) ? TransportFam.TCP : TransportFam.UDP;

        if (family == Families.AF_INET) {
            packet.networkFamily = NetFam.IPv4;
            packet.sourceAddress = decodeV4Address(buffer, (byte) (Fields.addr + Ipv4Addr.src_addr));
            packet.destinationAddress = decodeV4Address(buffer, (byte) (Fields.addr + Ipv4Addr.dst_addr));
            packet.sourcePort = decodeUInt8(buffer, (byte) (Fields.addr + Ipv4Addr.src_port));
            packet.destinationPort = decodeUInt8(buffer, (byte) (Fields.addr + Ipv4Addr.dst_port));
        } else if (family == Families.AF_INET6) {
            packet.networkFamily = NetFam.IPv6;
            packet.sourceAddress = decodeV6Address(buffer, (byte) (Fields.addr + Ipv6Addr.src_addr));
            packet.destinationAddress = decodeV6Address(buffer, (byte) (Fields.addr + Ipv6Addr.dst_addr));
            packet.sourcePort = decodeUInt8(buffer, (byte) (Fields.addr + Ipv6Addr.src_port));
            packet.destinationPort = decodeUInt8(buffer, (byte) (Fields.addr + Ipv6Addr.dst_port));
        }

        return packet;
    }

    /**
     * Build an PROXY protocol v2 conform header based on the given metadata.
     * @param transport {@link TransportFam} used to transmit the data.
     * @param srcAddress {@link InetAddress} that initiated the connection.
     * @param src_port Port the data was received from.
     * @param dstAddress {@link InetAddress} the connection was targeted at.
     * @param dst_port Port the data was received on.
     * @return Byte array containing the whole PROXY protocol v2 header including all metadata.
     */
    public static byte[] encode(TransportFam transport,
                                InetAddress srcAddress, int src_port, InetAddress dstAddress, int dst_port) {
        // Convert InetAddress objects to byte array representation
        byte[] src_address = srcAddress.getAddress();
        byte[] dst_address = dstAddress.getAddress();

        // Calculate header length for array creation
        int headerPayloadLength = src_address.length + dst_address.length + 4;
        int headerLength = sigBytes.length + 4 + headerPayloadLength;

        // Create a new byte array that get's filled with the header data step by step
        byte[] header = new byte[headerLength];
        int offset = 0;

        // Write protocol signature to header
        System.arraycopy(sigBytes, 0, header, offset, sigBytes.length);
        offset = offset + sigBytes.length;

        // Write protocol version to header (always v2)
        header[offset] = v2;
        offset++;


        // Write transport protocol family to header
        byte family;
        if (src_address.length == 16)  {
            family = Families.AF_INET6;
        } else {
            family = Families.AF_INET;
        }
        family = (transport == TransportFam.TCP) ? (byte) (family + 0x1) : (byte) (family + 0x2);
        header[offset] = family;
        offset++;

        // Write header payload length to header array
        ByteBuffer buffer = ByteBuffer.allocate(2);
        buffer.putShort((short) headerPayloadLength);
        System.arraycopy(buffer.array(), 0, header, offset, buffer.array().length);
        offset = offset + 2;

        // Write source address
        System.arraycopy(src_address, 0, header, offset, src_address.length);
        offset = offset + src_address.length;

        // Write destination address
        System.arraycopy(dst_address, 0, header, offset, dst_address.length);
        offset = offset + dst_address.length;

        // Write source port
        buffer = ByteBuffer.allocate(2);
        buffer.putShort((short) src_port);
        System.arraycopy(buffer.array(), 0, header, offset, buffer.array().length);
        offset = offset + 2;

        // Write destination port
        buffer = ByteBuffer.allocate(2);
        buffer.putShort((short) dst_port);
        System.arraycopy(buffer.array(), 0, header, offset, buffer.array().length);

        return header;
    }

    /**
     * Decode an uint8_t to an Java int.
     * @param buffer Byte array to read from.
     * @param offset Offset from which to start reading.
     * @return int with the value read from the data.
     */
    private static int decodeUInt8(byte[] buffer, byte offset) {
        return ((buffer[offset] & 0xFF) << 8) |
                ((buffer[offset + 1] & 0xFF));
    }

    /**
     * Decode an IPv4 address from byte representation to {@link InetAddress}.
     * @param buffer Byte array to read from.
     * @param offset Offset from which to start reading.
     * @return {@link InetAddress} representation of the IPv4 address.
     * @throws UnknownHostException Thrown if the read byte array can't be parsed.
     */
    private static InetAddress decodeV4Address(byte[] buffer, byte offset) throws UnknownHostException {
        byte[] address = new byte[4];
        System.arraycopy(buffer, offset, address, 0, 4);
        return InetAddress.getByAddress(address);
    }

    /**
     * Decode an IPv6 address from byte representation to {@link InetAddress}.
     * @param buffer Byte array to read from.
     * @param offset Offset from which to start reading.
     * @return {@link InetAddress} representation of the IPv6 address.
     * @throws UnknownHostException Thrown if the read byte array can't be parsed.
     */
    private static InetAddress decodeV6Address(byte[] buffer, byte offset) throws UnknownHostException {
        byte[] address = new byte[16];
        System.arraycopy(buffer, offset, address, 0, 16);
        return InetAddress.getByAddress(address);
    }
}
