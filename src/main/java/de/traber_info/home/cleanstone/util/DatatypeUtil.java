package de.traber_info.home.cleanstone.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Utility class to de-/encode the custom data types used by Minecraft's protocol.
 *
 * @author Oliver Traber
 */
public class DatatypeUtil {

    /** SLF4J logger for usage in this class */
    private static final Logger LOG = LoggerFactory.getLogger(DatatypeUtil.class.getName());

    /** Int holding the amount of bytes read by the last read function call. */
    private int bytesRead = 0;

    /**
     * Parse a Minecraft protocol VarInt to an Java int.
     * @param input Input byte array.
     * @param byteOffset Start offset in bytes.
     * @return Parsed Java int.
     */
    public int readVarInt(byte[] input, int byteOffset) {
        byte[] offsetInput = Arrays.copyOfRange(input, byteOffset, input.length);
        int numRead = 0;
        int result = 0;
        byte read;
        do {
            read = offsetInput[numRead];
            int value = (read & 0b01111111);
            result |= (value << (7 * numRead));

            numRead++;
            if (numRead > 5) {
                LOG.error("VarInt is too big!");
                return -1;
            }
        } while ((read & 0b10000000) != 0);

        bytesRead = numRead;
        return result;
    }

    /**
     * Parse a String from a Minecraft protocol package.
     * @param input Input byte array.
     * @param byteOffset Start offset in bytes.
     * @return Parsed Java string.
     */
    public String readString(byte[] input, int byteOffset) {
        int stringLength = readVarInt(input, byteOffset);
        int startOffset = byteOffset + getBytesRead();
        int end = startOffset + stringLength;
        bytesRead = end;
        return new String(Arrays.copyOfRange(input, startOffset, end), StandardCharsets.UTF_8);
    }

    /**
     * Get the count of bytes read by the last called read function.
     * @return Count of bytes read by the last called read function.
     */
    public int getBytesRead() {
        return bytesRead;
    }

}
