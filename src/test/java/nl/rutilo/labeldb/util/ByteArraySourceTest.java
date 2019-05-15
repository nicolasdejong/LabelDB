package nl.rutilo.labeldb.util;

import org.junit.Test;

import java.util.Arrays;

import static nl.rutilo.util.testsupport.TestUtils.DeletedWhenClosedFile;
import static nl.rutilo.util.testsupport.TestUtils.createTempFile;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class ByteArraySourceTest {

    @Test public void testHasMoreData() {
        final ByteArraySource bas = new ByteArraySource(new byte[0]);
        assertFalse(bas.hasMoreData());
        final ByteArraySource bas2 = new ByteArraySource(new byte[] { 1, 2, 3, 4 });
        assertTrue(bas2.hasMoreData());
        assertThat(bas2.getShort(), is((short)0x0102));
        assertTrue(bas2.hasMoreData());
        assertThat(bas2.getShort(), is((short)0x0304));
        assertFalse(bas2.hasMoreData());
    }
    @Test public void testGetLong() {
        final ByteArraySource bas = new ByteArraySource(
            new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 0, 0, 0, -1, 0x44, 0x55, 0x66, 0x77, 1, 2, 3, 4, 5, 6, 7 });
        assertThat(bas.getLong(), is(0x0102030405060708L));
        assertThat(bas.getLong(), is(0x000000FF44556677L));
        assertThat(bas.getLong(), is(-1L));
    }
    @Test public void testGetInt() {
        final ByteArraySource bas = new ByteArraySource(
            new byte[] { 1, 2, 3, 4, 0, -1, 0x44, 0x55, 1, 2, 3 });
        assertThat(bas.getInt(), is(0x01020304));
        assertThat(bas.getInt(), is(0x00FF4455));
        assertThat(bas.getInt(), is(-1));
    }
    @Test public void testGetShort() {
        final ByteArraySource bas = new ByteArraySource(
            new byte[] { 1, 2, 3, 4, -1, 0x44, 0x55 });
        assertThat(bas.getShort(), is((short)0x0102));
        assertThat(bas.getShort(), is((short)0x0304));
        assertThat(bas.getShort(), is((short)0xFF44));
        assertThat(bas.getShort(), is((short)-1));
    }

    @Test public void testGetLongs() {
        final byte[] bytes = new byte[] {
            1, 2, 3, 4, 5, 6, 7, 8,
            0, 0, 0, -1, 0x44, 0x55, 0x66, 0x77,
            1, 2, 3, 4, 5, 6, 7
        };
        final long[] longs1 = new long[1];
        final long[] longs2 = new long[2];
        final long[] longs3 = new long[3];


        Arrays.fill(longs1, -1);
        assertThat(new ByteArraySource(bytes).getLongs(longs1), is(new long[] { 0x0102030405060708L }));
        Arrays.fill(longs1, -1);
        assertThat(new ByteArraySource(bytes).getLongs(longs1, 0, 2), is(new long[] { 0x0102030405060708L }));

        Arrays.fill(longs2, -1);
        assertThat(new ByteArraySource(bytes).getLongs(longs2, 0, 1), is(new long[] { 0x0102030405060708L, -1 }));
        Arrays.fill(longs2, -1);
        assertThat(new ByteArraySource(bytes).getLongs(longs2, 1, 1), is(new long[] { -1, 0x0102030405060708L }));
        Arrays.fill(longs2, -1);
        assertThat(new ByteArraySource(bytes).getLongs(longs2, 1, 2), is(new long[] { -1, 0x0102030405060708L }));
        Arrays.fill(longs2, -1);
        assertThat(new ByteArraySource(bytes).getLongs(longs2, 0, 3), is(new long[] { 0x0102030405060708L, 0xFF44556677L }));
        Arrays.fill(longs2, -1);
        assertThat(new ByteArraySource(bytes).getLongs(longs2, 1, 3), is(new long[] { -1, 0x0102030405060708L }));

        Arrays.fill(longs3, -1);
        assertThat(new ByteArraySource(bytes).getLongs(longs3, 0, 3), is(new long[] { 0x0102030405060708L, 0xFF44556677L, -1 }));
        Arrays.fill(longs3, -1);
        assertThat(new ByteArraySource(bytes).getLongs(longs3, 1, 3), is(new long[] { -1, 0x0102030405060708L, 0xFF44556677L }));
    }
    @Test public void testGetInts() {
        final byte[] bytes = new byte[] {
            1, 2, 3, 4,
            0, -1, 0x44, 0x55,
            1, 2, 3
        };
        final int[] ints1 = new int[1];
        final int[] ints2 = new int[2];
        final int[] ints3 = new int[3];


        Arrays.fill(ints1, -1);
        assertThat(new ByteArraySource(bytes).getInts(ints1), is(new int[] { 0x01020304 }));
        Arrays.fill(ints1, -1);
        assertThat(new ByteArraySource(bytes).getInts(ints1, 0, 2), is(new int[] { 0x01020304 }));

        Arrays.fill(ints2, -1);
        assertThat(new ByteArraySource(bytes).getInts(ints2, 0, 1), is(new int[] { 0x01020304, -1 }));
        Arrays.fill(ints2, -1);
        assertThat(new ByteArraySource(bytes).getInts(ints2, 1, 1), is(new int[] { -1, 0x01020304 }));
        Arrays.fill(ints2, -1);
        assertThat(new ByteArraySource(bytes).getInts(ints2, 1, 2), is(new int[] { -1, 0x01020304 }));
        Arrays.fill(ints2, -1);
        assertThat(new ByteArraySource(bytes).getInts(ints2, 0, 3), is(new int[] { 0x01020304, 0xFF4455 }));
        Arrays.fill(ints2, -1);
        assertThat(new ByteArraySource(bytes).getInts(ints2, 1, 3), is(new int[] { -1, 0x01020304 }));

        Arrays.fill(ints3, -1);
        assertThat(new ByteArraySource(bytes).getInts(ints3, 0, 3), is(new int[] { 0x01020304, 0xFF4455, -1 }));
        Arrays.fill(ints3, -1);
        assertThat(new ByteArraySource(bytes).getInts(ints3, 1, 3), is(new int[] { -1, 0x01020304, 0xFF4455 }));
    }
    @Test public void testGetShorts() {
        final byte[] bytes = new byte[] {
            1, 2,
            0x44, -1
        };
        final short[] shorts1 = new short[1];
        final short[] shorts2 = new short[2];
        final short[] shorts3 = new short[3];

        Arrays.fill(shorts1, (short)-1);
        assertThat(new ByteArraySource(bytes).getShorts(shorts1), is(new short[] { 0x0102 }));
        Arrays.fill(shorts1, (short)-1);
        assertThat(new ByteArraySource(bytes).getShorts(shorts1, 0, 2), is(new short[] { 0x0102 }));

        Arrays.fill(shorts2, (short)-1);
        assertThat(new ByteArraySource(bytes).getShorts(shorts2, 0, 1), is(new short[] { 0x0102, -1 }));
        Arrays.fill(shorts2, (short)-1);
        assertThat(new ByteArraySource(bytes).getShorts(shorts2, 1, 1), is(new short[] { -1, 0x0102 }));
        Arrays.fill(shorts2, (short)-1);
        assertThat(new ByteArraySource(bytes).getShorts(shorts2, 1, 2), is(new short[] { -1, 0x0102 }));
        Arrays.fill(shorts2, (short)-1);
        assertThat(new ByteArraySource(bytes).getShorts(shorts2, 0, 3), is(new short[] { 0x0102, 0x44FF }));
        Arrays.fill(shorts2, (short)-1);
        assertThat(new ByteArraySource(bytes).getShorts(shorts2, 1, 3), is(new short[] { -1, 0x0102 }));

        Arrays.fill(shorts3, (short)-1);
        assertThat(new ByteArraySource(bytes).getShorts(shorts3, 0, 3), is(new short[] { 0x0102, 0x44FF, -1 }));
        Arrays.fill(shorts3, (short)-1);
        assertThat(new ByteArraySource(bytes).getShorts(shorts3, 1, 3), is(new short[] { -1, 0x0102, 0x44FF }));
    }

    @Test public void testCopyInto() {
        final byte[] bytes = { 1, 2, 3, 4 };
        try(final DeletedWhenClosedFile file = createTempFile()) {
            Utils.writeByteArrayToFile(file, bytes);
            final ByteArraySource bas = new ByteArraySource(file); // test file constructor
            final byte[] bytes2 = new byte[bytes.length];
            bas.copyInto(bytes2);
            assertThat(bytes, is(bytes2));
        }
        final ByteArraySource bas = new ByteArraySource(bytes);
        final byte[] bytes4 = new byte[4];
        bas.copyInto(bytes4);
        assertThat(bytes4, is(new byte[] { 1, 2, 3, 4 }));
    }
    @Test public void testCopyBytes() {
        final byte[] bytes = { -1, -2, -3, -4 };
        final byte[] copy = new ByteArraySource(bytes).copyBytes(2);
        assertThat(copy, is(new byte[] { -1, -2 }));
    }

    @Test public void testGetString() {
        final byte[] bytes = { 0, 3, 'a', 'b', 'c' };
        final String s = new ByteArraySource(bytes).getString();
        assertThat(s, is("abc"));
    }
}