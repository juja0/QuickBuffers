/*-
 * #%L
 * robobuf-runtime
 * %%
 * Copyright (C) 2019 HEBI Robotics
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

package us.hebi.robobuf;

import us.hebi.robobuf.ProtoUtil.Charsets;

/**
 * @author Florian Enner
 * @since 12 Nov 2019
 */
class JsonUtil {

    static class Base64Encoding {

        /**
         * RFC4648
         *
         * @param bytes
         * @param length
         * @param output
         */
        static void writeQuotedBase64(final byte[] bytes, final int length, RepeatedByte output) {

            // Size output buffer
            final int encodedLength = ((length + 2) / 3) << 2;
            int pos = output.addLength(encodedLength + 2 /* quotes */);
            final byte[] buffer = output.array;
            buffer[pos++] = '"';

            // Encode 24-bit blocks
            int i;
            final int blockableLength = (length / 3) * 3;
            for (i = 0; i < blockableLength; i += 3, pos += 4) {
                // Copy next three bytes into lower 24 bits of int
                final int bits = (bytes[i] & 0xff) << 16 | (bytes[i + 1] & 0xff) << 8 | (bytes[i + 2] & 0xff);

                // Encode the 24 bits into four 6 bit characters
                buffer[pos/**/] = BASE64[(bits >>> 18) & 0x3f];
                buffer[pos + 1] = BASE64[(bits >>> 12) & 0x3f];
                buffer[pos + 2] = BASE64[(bits >>> 6) & 0x3f];
                buffer[pos + 3] = BASE64[bits & 0x3f];
            }

            // Pad and encode last bits if source isn't even 24 bits
            final int remaining = length - blockableLength; // 0 - 2.
            if (remaining > 0) {
                // Prepare the int
                final int bits = ((bytes[i] & 0xff) << 10) | (remaining == 2 ? ((bytes[i + 1] & 0xff) << 2) : 0);

                // Set last four bytes
                buffer[pos/**/] = BASE64[bits >> 12];
                buffer[pos + 1] = BASE64[(bits >>> 6) & 0x3f];
                buffer[pos + 2] = remaining == 2 ? BASE64[bits & 0x3f] : (byte) '=';
                buffer[pos + 3] = '=';
                pos += 4;
            }

            buffer[pos] = '"';

        }

        private static final byte[] BASE64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".getBytes(Charsets.ISO_8859_1);

    }

    static class StringEncoding {

        static void writeRawAscii(CharSequence sequence, RepeatedByte output) {
            final int length = sequence.length();
            output.reserve(length);
            for (int i = 0; i < length; i++) {
                output.array[output.length++] = (byte) sequence.charAt(i);
            }
        }

        static void writeQuotedUtf8(CharSequence sequence, RepeatedByte output) {
            // Fast-path: no utf8 and escape support
            final int numChars = sequence.length();
            final int pos = output.addLength(numChars + 2) + 1;
            output.array[pos - 1] = '"';
            int i = 0;
            fastpath:
            {
                for (; i < numChars; i++) {
                    final char c = sequence.charAt(i);
                    if (c < 128 && CAN_DIRECT_WRITE[c]) {
                        output.array[pos + i] = (byte) c;
                    } else {
                        output.length = pos + i;
                        break fastpath;
                    }
                }
                output.array[pos + i] = '"';
                return;
            }

            // Slow-path: utf8 and/or escaping
            for (; i < numChars; i++) {
                final char c = sequence.charAt(i);

                if (c < 0x80) { // ascii
                    if (CAN_DIRECT_WRITE[c]) {
                        final int offset = output.addLength(1);
                        output.array[offset] = (byte) c;
                    } else {
                        writeEscapedAscii(c, output);
                    }

                } else if (c < 0x800) { // 11 bits, two UTF-8 bytes
                    final int offset = output.addLength(2);
                    output.array[offset/**/] = (byte) ((0xF << 6) | (c >>> 6));
                    output.array[offset + 1] = (byte) (0x80 | (0x3F & c));

                } else if ((c < Character.MIN_SURROGATE || Character.MAX_SURROGATE < c)) {
                    // Maximum single-char code point is 0xFFFF, 16 bits, three UTF-8 bytes
                    final int offset = output.addLength(3);
                    output.array[offset/**/] = (byte) ((0xF << 5) | (c >>> 12));
                    output.array[offset + 1] = (byte) (0x80 | (0x3F & (c >>> 6)));
                    output.array[offset + 2] = (byte) (0x80 | (0x3F & c));

                } else {
                    // Minimum code point represented by a surrogate pair is 0x10000, 17 bits, four UTF-8 bytes
                    final char low;
                    if (i + 1 == numChars || !Character.isSurrogatePair(c, (low = sequence.charAt(++i)))) {
                        throw new IllegalArgumentException("Unpaired surrogate at index " + (i - 1));
                    }
                    int codePoint = Character.toCodePoint(c, low);
                    final int offset = output.addLength(4);
                    output.array[offset/**/] = (byte) ((0xF << 4) | (codePoint >>> 18));
                    output.array[offset + 1] = (byte) (0x80 | (0x3F & (codePoint >>> 12)));
                    output.array[offset + 2] = (byte) (0x80 | (0x3F & (codePoint >>> 6)));
                    output.array[offset + 3] = (byte) (0x80 | (0x3F & codePoint));
                }
            }

            output.reserve(1);
            output.array[output.length++] = (byte) '"';

        }

        private static void writeEscapedAscii(char c, RepeatedByte output) {
            final byte escapeChar = ESCAPE_CHAR[c];
            if (escapeChar != 0) {
                // escaped with slash, e.g., \\t
                final int pos = output.addLength(2);
                output.array[pos] = '\\';
                output.array[pos + 1] = escapeChar;
            } else {
                // slash-U escaping, e.g., control character
                final int pos = output.addLength(6);
                output.array[pos] = '\\';
                output.array[pos + 1] = 'u';
                output.array[pos + 2] = ITOA[c >> 12 & 0xf];
                output.array[pos + 3] = ITOA[c >> 8 & 0xf];
                output.array[pos + 4] = ITOA[c >> 4 & 0xf];
                output.array[pos + 5] = ITOA[c & 0xf];
            }
        }

        private static final byte[] ITOA = new byte[]{
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                'a', 'b', 'c', 'd', 'e', 'f'};

        private static final boolean[] CAN_DIRECT_WRITE = new boolean[128];
        private static final byte[] ESCAPE_CHAR = new byte[128];

        static {
            for (int i = 0; i < CAN_DIRECT_WRITE.length; i++) {
                if (i > 31 && i < 126 && i != '"' && i != '\\') {
                    CAN_DIRECT_WRITE[i] = true;
                }
            }
            ESCAPE_CHAR['"'] = '"';
            ESCAPE_CHAR['\\'] = '\\';
            ESCAPE_CHAR['\b'] = 'b';
            ESCAPE_CHAR['\f'] = 'f';
            ESCAPE_CHAR['\n'] = 'n';
            ESCAPE_CHAR['\r'] = 'r';
            ESCAPE_CHAR['\t'] = 't';
        }

    }

    /**
     * Copied from JsonIter / DSL-Platform
     * https://github.com/json-iterator/java/blob/master/src/main/java/com/jsoniter/output/StreamImplNumber.java
     */
    static class NumberEncoding {

        private final static int[] DIGITS = new int[1000];

        static {
            for (int i = 0; i < 1000; i++) {
                DIGITS[i] = (i < 10 ? (2 << 24) : i < 100 ? (1 << 24) : 0)
                        + (((i / 100) + '0') << 16)
                        + ((((i / 10) % 10) + '0') << 8)
                        + i % 10 + '0';
            }
        }

        public static void writeInt(int value, final RepeatedByte output) {
            output.reserve(12);
            int pos = output.length;
            final byte[] buf = output.array;
            if (value < 0) {
                if (value == Integer.MIN_VALUE) {
                    System.arraycopy(MIN_INT, 0, buf, pos, MIN_INT.length);
                    output.length = pos + MIN_INT.length;
                    return;
                }
                value = -value;
                buf[pos++] = '-';
            }
            final int q1 = value / 1000;
            if (q1 == 0) {
                pos += writeFirstBuf(buf, DIGITS[value], pos);
                output.length = pos;
                return;
            }
            final int r1 = value - q1 * 1000;
            final int q2 = q1 / 1000;
            if (q2 == 0) {
                final int v1 = DIGITS[r1];
                final int v2 = DIGITS[q1];
                int off = writeFirstBuf(buf, v2, pos);
                writeBuf(buf, v1, pos + off);
                output.length = pos + 3 + off;
                return;
            }
            final int r2 = q1 - q2 * 1000;
            final long q3 = q2 / 1000;
            final int v1 = DIGITS[r1];
            final int v2 = DIGITS[r2];
            if (q3 == 0) {
                pos += writeFirstBuf(buf, DIGITS[q2], pos);
            } else {
                final int r3 = (int) (q2 - q3 * 1000);
                buf[pos++] = (byte) (q3 + '0');
                writeBuf(buf, DIGITS[r3], pos);
                pos += 3;
            }
            writeBuf(buf, v2, pos);
            writeBuf(buf, v1, pos + 3);
            output.length = pos + 6;
        }

        private static int writeFirstBuf(final byte[] buf, final int v, int pos) {
            final int start = v >> 24;
            if (start == 0) {
                buf[pos++] = (byte) (v >> 16);
                buf[pos++] = (byte) (v >> 8);
            } else if (start == 1) {
                buf[pos++] = (byte) (v >> 8);
            }
            buf[pos] = (byte) v;
            return 3 - start;
        }

        private static void writeBuf(final byte[] buf, final int v, int pos) {
            buf[pos] = (byte) (v >> 16);
            buf[pos + 1] = (byte) (v >> 8);
            buf[pos + 2] = (byte) v;
        }

        public static final void writeLong(long value, final RepeatedByte output) {
            output.reserve(22);
            int pos = output.length;
            final byte[] buf = output.array;
            if (value < 0) {
                if (value == Long.MIN_VALUE) {
                    System.arraycopy(MIN_LONG, 0, buf, pos, MIN_LONG.length);
                    output.length = pos + MIN_LONG.length;
                    return;
                }
                value = -value;
                buf[pos++] = '-';
            }
            final long q1 = value / 1000;
            if (q1 == 0) {
                pos += writeFirstBuf(buf, DIGITS[(int) value], pos);
                output.length = pos;
                return;
            }
            final int r1 = (int) (value - q1 * 1000);
            final long q2 = q1 / 1000;
            if (q2 == 0) {
                final int v1 = DIGITS[r1];
                final int v2 = DIGITS[(int) q1];
                int off = writeFirstBuf(buf, v2, pos);
                writeBuf(buf, v1, pos + off);
                output.length = pos + 3 + off;
                return;
            }
            final int r2 = (int) (q1 - q2 * 1000);
            final long q3 = q2 / 1000;
            if (q3 == 0) {
                final int v1 = DIGITS[r1];
                final int v2 = DIGITS[r2];
                final int v3 = DIGITS[(int) q2];
                pos += writeFirstBuf(buf, v3, pos);
                writeBuf(buf, v2, pos);
                writeBuf(buf, v1, pos + 3);
                output.length = pos + 6;
                return;
            }
            final int r3 = (int) (q2 - q3 * 1000);
            final int q4 = (int) (q3 / 1000);
            if (q4 == 0) {
                final int v1 = DIGITS[r1];
                final int v2 = DIGITS[r2];
                final int v3 = DIGITS[r3];
                final int v4 = DIGITS[(int) q3];
                pos += writeFirstBuf(buf, v4, pos);
                writeBuf(buf, v3, pos);
                writeBuf(buf, v2, pos + 3);
                writeBuf(buf, v1, pos + 6);
                output.length = pos + 9;
                return;
            }
            final int r4 = (int) (q3 - q4 * 1000);
            final int q5 = q4 / 1000;
            if (q5 == 0) {
                final int v1 = DIGITS[r1];
                final int v2 = DIGITS[r2];
                final int v3 = DIGITS[r3];
                final int v4 = DIGITS[r4];
                final int v5 = DIGITS[q4];
                pos += writeFirstBuf(buf, v5, pos);
                writeBuf(buf, v4, pos);
                writeBuf(buf, v3, pos + 3);
                writeBuf(buf, v2, pos + 6);
                writeBuf(buf, v1, pos + 9);
                output.length = pos + 12;
                return;
            }
            final int r5 = q4 - q5 * 1000;
            final int q6 = q5 / 1000;
            final int v1 = DIGITS[r1];
            final int v2 = DIGITS[r2];
            final int v3 = DIGITS[r3];
            final int v4 = DIGITS[r4];
            final int v5 = DIGITS[r5];
            if (q6 == 0) {
                pos += writeFirstBuf(buf, DIGITS[q5], pos);
            } else {
                final int r6 = q5 - q6 * 1000;
                buf[pos++] = (byte) (q6 + '0');
                writeBuf(buf, DIGITS[r6], pos);
                pos += 3;
            }
            writeBuf(buf, v5, pos);
            writeBuf(buf, v4, pos + 3);
            writeBuf(buf, v3, pos + 6);
            writeBuf(buf, v2, pos + 9);
            writeBuf(buf, v1, pos + 12);
            output.length = pos + 15;
        }

        /**
         * Writes a floating point number with up to 6 digit after comma precision
         *
         * @param val
         * @param output
         */
        public static void writeFloat(float val, final RepeatedByte output) {
            if (val < 0) {
                if (val == Float.NEGATIVE_INFINITY) {
                    output.addAll(NEGATIVE_INF);
                    return;
                }
                output.add((byte) '-');
                val = -val;
            }
            if (val > 0x4ffffff) {
                if (val == Float.POSITIVE_INFINITY) {
                    output.addAll(POSITIVE_INF);
                    return;
                }
                StringEncoding.writeRawAscii(Float.toString(val), output);
                return;
            } else if (Float.isNaN(val)) {
                output.addAll(NAN);
                return;
            }
            final long lval = (long) (val * exp6 + 0.5);
            writeLong(lval / exp6, output);
            int fval = (int) lval % exp6;
            if (fval == 0) {
                return;
            }
            output.reserve(12);
            output.array[output.length++] = '.';
            for (int p = precision6 - 1; p > 0 && fval < POW10[p]; p--) {
                output.array[output.length++] = '0';
            }
            writeInt(fval, output);
            while (output.array[output.length - 1] == '0') {
                output.length--;
            }
        }

        /**
         * Writes a floating point number with up to 9 digit after comma precision
         *
         * @param val
         * @param output
         */
        public static void writeDouble(double val, final RepeatedByte output) {
            if (val < 0) {
                if (val == Double.NEGATIVE_INFINITY) {
                    output.addAll(NEGATIVE_INF);
                    return;
                }
                val = -val;
                output.add((byte) '-');
            }
            if (val > 0x4ffffff) {
                if (val == Double.POSITIVE_INFINITY) {
                    output.addAll(POSITIVE_INF);
                    return;
                }
                StringEncoding.writeRawAscii(Double.toString(val), output);
                return;
            } else if (Double.isNaN(val)) {
                output.addAll(NAN);
                return;
            }
            final long lval = (long) (val * exp9 + 0.5);
            writeLong(lval / exp9, output);
            long fval = (long) lval % exp9;
            if (fval == 0) {
                return;
            }
            output.reserve(15);
            output.array[output.length++] = '.';
            for (int p = precision9 - 1; p > 0 && fval < POW10[p]; p--) {
                output.array[output.length++] = '0';
            }
            writeLong(fval, output);
            while (output.array[output.length - 1] == '0') {
                output.length--;
            }
        }

        private static final int precision6 = 6;
        private static final int precision9 = 9;
        private static final int exp6 = 1000000; // 10^6
        private static final int exp9 = 1000000000; // 10^9
        private static final int[] POW10 = {1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000, 100000000};

        // JSON doesn't define -inf etc., so encode as String
        private static final byte[] NEGATIVE_INF = "\"-Infinity\"".getBytes(Charsets.ISO_8859_1);
        private static final byte[] POSITIVE_INF = "\"Infinity\"".getBytes(Charsets.ISO_8859_1);
        private static final byte[] NAN = "\"NaN\"".getBytes(Charsets.ISO_8859_1);
        private static final byte[] MIN_INT = "-2147483648".getBytes();
        private static final byte[] MIN_LONG = "-9223372036854775808".getBytes();

    }

    static class BooleanEncoding {

        static void writeBoolean(boolean value, RepeatedByte output) {
            if (value) {
                output.addAll(TRUE_BYTES);
            } else {
                output.addAll(FALSE_BYTES);
            }
        }

        private static final byte[] TRUE_BYTES = "true".getBytes(Charsets.ISO_8859_1);
        private static final byte[] FALSE_BYTES = "false".getBytes(Charsets.ISO_8859_1);

    }

}