package de.traber_info.home.cleanstone.model.object;

import de.traber_info.home.cleanstone.util.DatatypeUtil;
import java.util.Arrays;

/**
 * Packet class used to parse and strip the packets id from the raw packet byte array.
 *
 * @author Oliver Traber
 */
public class Packet {

    /** Id of the packet */
    private int packetId;

    /** Packet data without the packets id */
    private byte[] unreadData;

    /** Full packet data including the packets id */
    private byte[] fullPacket;

    /**
     * Create a new Packet instance.
     * @param rawData Raw packet data which should be parsed.
     */
    public Packet(byte[] rawData) {
        DatatypeUtil datatypeUtil = new DatatypeUtil();
        int packetLength = datatypeUtil.readVarInt(rawData, 0);
        fullPacket = Arrays.copyOfRange(rawData, 1, packetLength);
        packetId = datatypeUtil.readVarInt(fullPacket, 0);
        unreadData = Arrays.copyOfRange(fullPacket, datatypeUtil.getBytesRead(), rawData.length);
    }

    /**
     * Get the id of the packet.
     * @return Id of the packet.
     */
    public int getPacketId() {
        return packetId;
    }

    /**
     * Get the data of the packet without the packets id.
     * @return Data of the packet without the packets id.
     */
    public byte[] getUnreadData() {
        return unreadData;
    }

    /**
     * Get the full packet data including the packets id.
     * @return Full packet data including the packets id.
     */
    public byte[] getFullPacket() {
        return fullPacket;
    }
}
