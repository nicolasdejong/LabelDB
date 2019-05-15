package nl.rutilo.labeldb;

import org.junit.Test;

import java.io.File;

import static nl.rutilo.util.testsupport.TestUtils.*;
import static nl.rutilo.util.testsupport.TestUtils.createTempDir;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class LongsTest {

    @Test public void testSetGet() {
        final Longs longs = new Longs();
        assertThat(longs.isDirty(), is(false));

        for(int id=0; id<1000; id++) {
            assertFalse("id " + id + " fails", longs.isSet(id));
            longs.set(id, id+5);
            assertThat(longs.get(id), is((long)id+5));
            assertTrue(longs.isSet(id));
        }
        assertThat(longs.get(1000), is(Longs.VALUE_UNUSED));
        assertThat(longs.isDirty(), is(true));

        longs.clear();
        assertFalse(longs.isSet(-1));
        assertFalse(longs.isSet(0));
        assertThat(longs.get(-1), is(Longs.VALUE_UNUSED));
        assertThat(longs.get(0), is(Longs.VALUE_UNUSED));
    }
    @Test public void testUnsetAndFirstIndex() {
        final Longs longs = new Longs();
        assertThat(longs.getFirstUnsetIndex(), is(0));

        longs.set(0, 1);
        assertThat(longs.getFirstUnsetIndex(), is(1));

        for(int i=1; i<Longs.DEFAULT_GROWTH_RATE; i++) longs.set(i, i);
        assertThat(longs.getFirstUnsetIndex(), is(Longs.DEFAULT_GROWTH_RATE));

        longs.unset(4, 6);
        assertThat(longs.getFirstUnsetIndex(), is(4));

        longs.set(4, 123);
        assertThat(longs.getFirstUnsetIndex(), is(6));

        longs.set(6, 678);
        assertThat(longs.getFirstUnsetIndex(), is(Longs.DEFAULT_GROWTH_RATE));
    }
    @Test public void testLoadAndStore() {
        try(final DeletedWhenClosedFile tempDir = createTempDir()) {
            final Longs longs = new Longs("test", tempDir);
            for(int i=0; i<1000; i++) longs.set(i, i);
            longs.store();
            assertThat(longs.isDirty(), is(false));

            longs.set(500, 1);
            longs.set(1500, 2);
            longs.set(550, 3);
            assertThat(longs.isDirty(), is(true));
            longs.store();
            assertThat(longs.isDirty(), is(false));

            final Longs longs2 = new Longs("test", tempDir);
            assertThat(longs2.isDirty(), is(false));
            for(int i=0; i<1000; i++) {
                assertThat(longs2.get(i), is(longs.get(i)));
            }
            assertThat(longs2.isDirty(), is(false));

            longs.clear();
            assertThat(longs.isDirty(), is(true));
            longs.store();
            assertThat(longs.isDirty(), is(false));
            assertThat(new File(tempDir, "test").length(), is(0L));

            longs.load();
            assertThat(longs.isDirty(), is(false));
            assertThat(longs.get(0), is(Longs.VALUE_UNUSED));
            for(int i=0; i<100; i++) longs.set(i, i);
            longs.store();

            longs.load();
            longs.clear();
            longs.set(1,1);
            longs.store();

            longs.load();
            assertThat(longs.get(1), is(1L));
            assertThat(longs.get(2), is(Longs.VALUE_UNUSED));

            final Longs longsIC = new Longs("name:with;illegal/\\chars", tempDir);
            longsIC.set(10, 111);
            longsIC.store();
            longsIC.load();
            assertThat(longsIC.get(10), is(111L));
            try {
                new Longs("", tempDir).set(1, 1).store();
                fail("Expected exception");
            } catch(final IllegalStateException e) {
                assertThat(e.getMessage(), containsString("needs to have a name"));
            }

            final Longs longsRO = new Longs("testRO", tempDir);
            longsRO.set(12,34);
            longsRO.store();
            final File roFile = new File(tempDir, "name_test^r^o");
            roFile.setWritable(false);
            longsRO.set(56,78);
            try {
                longsRO.store();
                fail("Expected throw: the file to write to is read-only");
            } catch(final RuntimeException e) {
                assertThat(e.getMessage(), containsString("Unable to save longs"));
                assertThat(e.getCause().getMessage(), containsString("Access is denied"));
            }
        }
    }
    @Test public void testGetIndexRangesToStore() {
        final int idBlockSize = (Longs.SAVE_BLOCK_SIZE_KB * 1024) / Long.BYTES; // 512
        final int useGrowthRate = idBlockSize * 10;
        final Longs longs = new Longs("", 0);
        longs.setGrowthRate(useGrowthRate);
        assertThat(longs.getIndexRangesToStore().length, is(0));

        longs.set(1, 1);
        assertThat(longs.getIndexRangesToStore().length, is(1));
        assertThat(longs.getIndexRangesToStore(), is(new int[][] { { 0, idBlockSize } }));
        longs.set(2, 2);
        assertThat(longs.getIndexRangesToStore(), is(new int[][] { { 0, idBlockSize } }));
        longs.set(idBlockSize-1, 3);
        assertThat(longs.getIndexRangesToStore(), is(new int[][] { { 0, idBlockSize } }));
        longs.set(idBlockSize, 4);
        assertThat(longs.getIndexRangesToStore(), is(new int[][] { { 0, 2*idBlockSize } }));
        longs.set(idBlockSize*3, 5);
        assertThat(longs.getIndexRangesToStore(), is(new int[][] { { 0, 2*idBlockSize }, { 3*idBlockSize, idBlockSize } }));
        longs.set(idBlockSize*5+10, 6);
        assertThat(longs.getIndexRangesToStore(), is(new int[][] { { 0, 2*idBlockSize }, { 3*idBlockSize, idBlockSize }, { 5*idBlockSize, idBlockSize } }));

        longs.clear();
        longs.set(idBlockSize*3, 7);
        assertThat(longs.getIndexRangesToStore(), is(new int[][] { { 3*idBlockSize, idBlockSize } }));

        longs.clear();
        for(int i = 0; i<useGrowthRate; i++) longs.set(i, i);
        assertThat(longs.getIndexRangesToStore(), is(new int[][] { { 0, useGrowthRate} }));
    }
    @Test public void testAsBits() {
        final Longs longs = new Longs("", 100);

        assertThat(longs.asBits().countSetBits(), is(0));
        longs.set(10, 5);
        longs.set(20, 8);
        longs.set(30, 12);
        assertThat(longs.asBits().countSetBits(), is(3));
        assertThat(longs.asBits(0, 10).countSetBits(), is(2));
        assertThat(longs.asBits(6, 12).countSetBits(), is(2));

        final Bits bits = longs.asBits();
        for(int i=0; i<100; i++) {
            assertThat(bits.isSet(i), is(i == 10 || i == 20 || i == 30));
        }
    }
}