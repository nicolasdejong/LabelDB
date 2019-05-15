package nl.rutilo.labeldb.util;

import nl.rutilo.labeldb.util.ByteArraySource;
import nl.rutilo.labeldb.util.ByteArrayTarget;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ByteArrayTargetTest {

    @Test public void testConstructors() {
        final ByteArrayTarget batNoArg = new ByteArrayTarget();
        batNoArg.add(0x1234);
        assertThat(batNoArg.toByteArray(), is(new byte[] { 0, 0, 0x12, 0x34 }));

        final ByteArrayTarget batBytes = new ByteArrayTarget(new byte[5]);
        batBytes.add(0x1234);
        assertThat(batBytes.toByteArray(), is(new byte[] { 0, 0, 0x12, 0x34, 0 }));

        final ByteArrayTarget batSized = new ByteArrayTarget(5);
        batSized.add(0x1234);
        assertThat(batSized.toByteArray(), is(new byte[] { 0, 0, 0x12, 0x34, 0 }));
    }

    @Test public void testAddValue() {
        final ByteArrayTarget bat = new ByteArrayTarget(); // Uses ByteArrayOutputStream
        bat.add(0x1234567876543210L);
        bat.add(0x12345678);
        bat.add((short)0x1234);
        bat.add((byte)0x12);
        assertThat(bat.toByteArray(), is(new byte[] {
            0x12, 0x34, 0x56, 0x78, 0x76, 0x54, 0x32, 0x10,
            0x12, 0x34, 0x56, 0x78,
            0x12, 0x34,
            0x12
        }));
        final ByteArrayTarget bat2 = new ByteArrayTarget(new byte[bat.toByteArray().length]);
        bat2.add(0x1234567876543210L);
        bat2.add(0x12345678);
        bat2.add((short)0x1234);
        bat2.add((byte)0x12);
        assertThat(bat2.toByteArray(), is(new byte[] {
            0x12, 0x34, 0x56, 0x78, 0x76, 0x54, 0x32, 0x10,
            0x12, 0x34, 0x56, 0x78,
            0x12, 0x34,
            0x12
        }));
    }

    @Test public void testAddLongs() {
        for(int type=0; type<2; type++) {
            final long[] values = {0xFEDCBA9876543210L, 0x112233, 0xCAFEBABE0DEADF00L, 999, -1};
            final ByteArrayTarget bat = type == 0 ? new ByteArrayTarget() : new ByteArrayTarget((values.length+3)*8);
            bat.add(values);
            bat.add(values, 3, 1);
            bat.add(values, 0, 2);
            final ByteArraySource bas = new ByteArraySource(bat.toByteArray());
            assertThat(bas.getLongs(new long[values.length]), is(values));
            assertThat(bas.getLongs(new long[1]), is(new long[]{999}));
            assertThat(bas.getLongs(new long[2]), is(new long[]{0xFEDCBA9876543210L, 0x112233}));
        }
    }
    @Test public void testAddInts() {
        for(int type=0; type<2; type++) {
            final int[] values = {0xFEDCBA98, 0x112233, 0xCAFEBABE, 999, -1};
            final ByteArrayTarget bat = type == 0 ? new ByteArrayTarget() : new ByteArrayTarget((values.length+3)*4);
            bat.add(values);
            bat.add(values, 3, 1);
            bat.add(values, 0, 2);
            final ByteArraySource bas = new ByteArraySource(bat.toByteArray());
            assertThat(bas.getInts(new int[values.length]), is(values));
            assertThat(bas.getInts(new int[1]), is(new int[]{999}));
            assertThat(bas.getInts(new int[2]), is(new int[]{0xFEDCBA98, 0x112233}));
        }
    }
    @Test public void testAddShorts() {
        for(int type=0; type<2; type++) {
            final short[] values = {(short)0xFEDC, 0x1122, (short)0xCAFE, 999, -1};
            final ByteArrayTarget bat = type == 0 ? new ByteArrayTarget() : new ByteArrayTarget((values.length+3)*2);
            bat.add(values);
            bat.add(values, 3, 1);
            bat.add(values, 0, 2);
            final ByteArraySource bas = new ByteArraySource(bat.toByteArray());
            assertThat(bas.getShorts(new short[values.length]), is(values));
            assertThat(bas.getShorts(new short[1]), is(new short[]{999}));
            assertThat(bas.getShorts(new short[2]), is(new short[]{(short)0xFEDC, 0x1122}));
        }
    }
    @Test public void testAddBytes() {
        for(int type=0; type<2; type++) {
            final byte[] values = {1,2,3,4,-1};
            final ByteArrayTarget bat = type == 0 ? new ByteArrayTarget() : new ByteArrayTarget(values.length+3);
            bat.add(values);
            bat.add(values, 3, 1);
            bat.add(values, 0, 2);
            final ByteArraySource bas = new ByteArraySource(bat.toByteArray());
            assertThat(bas.copyInto(new byte[values.length]), is(values));
            assertThat(bas.copyInto(new byte[1]), is(new byte[]{4}));
            assertThat(bas.copyInto(new byte[2]), is(new byte[]{1,2}));
        }
    }

    @Test public void testAddString() {
        for(int type=0; type<2; type++) {
            final String text = "abcdef";
            final ByteArrayTarget bat = type == 0 ? new ByteArrayTarget() : new ByteArrayTarget(text.length() + 2);
            bat.add(text);
            final ByteArraySource bas = new ByteArraySource(bat.toByteArray());
            assertThat(bas.getString(), is(text));
        }
    }
}