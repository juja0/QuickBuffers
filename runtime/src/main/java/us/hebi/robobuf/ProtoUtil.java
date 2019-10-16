package us.hebi.robobuf;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Utility methods for working with protobuf messages
 *
 * @author Florian Enner
 * @since 09 Aug 2019
 */
public class ProtoUtil {

    /**
     * Encode and write a varint to an OutputStream.  {@code value} is
     * treated as unsigned, so it won't be sign-extended if negative.
     *
     * The following is equal to Protobuf-Java's "msg.writeDelimitedTo(output)"
     *
     * byte[] data = msg.toByteArray();
     * writeRawVarint32(data.length, output);
     * output.write(data);
     *
     * @param value  int32 value to be encoded as varint
     * @param output target stream
     * @return number of written bytes
     */
    public static int writeRawVarint32(int value, OutputStream output) throws IOException {
        int numBytes = 1;
        while (true) {
            if ((value & ~0x7F) == 0) {
                output.write(value);
                return numBytes;
            } else {
                output.write((value & 0x7F) | 0x80);
                value >>>= 7;
                numBytes++;
            }
        }
    }

    /**
     * Reads and decodes a varint from the given input stream. If larger than 32
     * bits, discard the upper bits.
     *
     * The following is equal to Protobuf-Java's "msg.readDelimitedFrom(input)"
     *
     * int length = readRawVarint32(input);
     * byte[] data = new byte[length];
     * if(input.readData(data) != length) {
     * throw new IOException("truncated message");
     * }
     * return MyMessage.parseFrom(data);
     *
     * @param input source stream
     * @return value of the decoded varint
     */
    public static int readRawVarint32(InputStream input) throws IOException {
        byte tmp = readRawByte(input);
        if (tmp >= 0) {
            return tmp;
        }
        int result = tmp & 0x7f;
        if ((tmp = readRawByte(input)) >= 0) {
            result |= tmp << 7;
        } else {
            result |= (tmp & 0x7f) << 7;
            if ((tmp = readRawByte(input)) >= 0) {
                result |= tmp << 14;
            } else {
                result |= (tmp & 0x7f) << 14;
                if ((tmp = readRawByte(input)) >= 0) {
                    result |= tmp << 21;
                } else {
                    result |= (tmp & 0x7f) << 21;
                    result |= (tmp = readRawByte(input)) << 28;
                    if (tmp < 0) {
                        // Discard upper 32 bits.
                        for (int i = 0; i < 5; i++) {
                            if (readRawByte(input) >= 0) {
                                return result;
                            }
                        }
                        throw InvalidProtocolBufferException.malformedVarint();
                    }
                }
            }
        }
        return result;
    }

    private static byte readRawByte(InputStream input) throws IOException {
        int value = input.read();
        if (value < 0) {
            throw InvalidProtocolBufferException.truncatedMessage();
        }
        return (byte) (value);
    }

    /**
     * Compares whether the contents of two CharSequences are equal
     *
     * @param a sequence A
     * @param b sequence B
     * @return true if the contents of both sequences are the same
     */
    public static boolean isEqual(CharSequence a, CharSequence b) {
        if (a.length() != b.length())
            return false;

        for (int i = 0; i < a.length(); i++) {
            if (a.charAt(i) != b.charAt(i))
                return false;
        }

        return true;
    }


    // =========== Internal utility methods used by the runtime API ===========

    static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    static boolean isEqual(boolean a, boolean b) {
        return a == b;
    }

    static boolean isEqual(byte a, byte b) {
        return a == b;
    }

    static boolean isEqual(int a, int b) {
        return a == b;
    }

    static boolean isEqual(long a, long b) {
        return a == b;
    }

    static boolean isEqual(float a, float b) {
        return Float.floatToIntBits(a) == Float.floatToIntBits(b);
    }

    static boolean isEqual(double a, double b) {
        return Double.doubleToLongBits(a) == Double.doubleToLongBits(b);
    }

    private ProtoUtil() {
    }

}