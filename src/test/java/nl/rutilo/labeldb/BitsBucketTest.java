package nl.rutilo.labeldb;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class BitsBucketTest {

    @Test public void testSet() {
        final BitsBucket bucket = new BitsBucket();
        final int[] ids = { 0, 1, 6, 500, 1234, 2300, 5000, 5300, 7261, 8190 };
        for(final int id : ids) bucket.set(id, true);

        assertThat(bucket.getIndices(), is(ids));
    }
    @Test public void testSetAndIsSet() {
        final BitsBucket bucket = new BitsBucket();
        final int testBits = 200;

        for(int i=0; i<testBits; i++) {
            assertFalse(bucket.isSet(i));
            bucket.set(i, true);
            for(int j=0; j<testBits; j++) {
                assertThat(bucket.isSet(j), is(j == i));
            }
            bucket.set(i, false);
            for(int j=0; j<testBits; j++) {
                assertFalse(bucket.isSet(j));
            }
        }
        bucket.set(50, true);
        bucket.set(2000, true);
        bucket.set(8000, true);
        for(int j=0; j<bucket.size(); j++) {
            assertThat(bucket.isSet(j), is(j == 50 || j == 2000 || j == 8000));
        }
        try {
            bucket.set(bucket.size(), true);
            fail("Expected trow");
        } catch(final IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("Index outside"));
        }
    }
    @Test public void testIsAnySet() {
        final BitsBucket bucket = new BitsBucket();
        assertFalse(bucket.isAnySet());
        assertFalse(bucket.isAnySet(0, Bits.BUCKET_BITS_COUNT));
        for(int index=0; index<Bits.BUCKET_BITS_COUNT; index++) {
            bucket.clear();
            bucket.set(index, true);
            assertTrue("failed for full range: " + index, bucket.isAnySet(0, Bits.BUCKET_BITS_COUNT));
            assertTrue("failed for index,index " + index, bucket.isAnySet(index, index));
            assertTrue("failed for >=from "      + index, bucket.isAnySet(index, Bits.BUCKET_BITS_COUNT));
            assertTrue("failed for <=from "      + index, bucket.isAnySet(0, index));
            assertFalse("failed for <from "      + index, bucket.isAnySet(0, index-1));
            assertFalse("failed for >from "      + index, bucket.isAnySet(index+1, Bits.BUCKET_BITS_COUNT));
        }
    }
    @Test public void testReverse() {
        final BitsBucket bucket = new BitsBucket();
        assertThat(bucket.countSetBits(), is(0));
        bucket.reverse();
        assertThat(bucket.countSetBits(), is(bucket.size()));
        bucket.reverse();
        assertThat(bucket.countSetBits(), is(0));
        bucket.set(1, true);
        bucket.set(256, true);
        assertThat(bucket.countSetBits(), is(2));
        bucket.reverse();
        assertThat(bucket.countSetBits(), is(bucket.size()-2));
        assertThat(bucket.isSet(1), is(false));
        assertThat(bucket.isSet(256), is(false));
    }
    @Test public void testCountSetBits() {
        final BitsBucket bucket = new BitsBucket();
        final int[] ids = { 0, 1, 6, 500, 1234, 2300, 5000, 5300, 7261, 8190 };
        for(final int id : ids) bucket.set(id, true);
        assertThat(bucket.countSetBits(), is(ids.length));
    }
    @Test public void testCountOverlapWith() {
        final BitsBucket bucket1 = new BitsBucket();
        final BitsBucket bucket2 = new BitsBucket();
        final int[] ids1 = { 0, 1, 6, 500, 1234, 2300, 5000, 5300, 7261, 8190 };
        final int[] ids2 = { 0, 2, 5, 500, 1000, 2300, 3000, 4999, 5300 };
        for(final int id : ids1) bucket1.set(id, true);
        for(final int id : ids2) bucket2.set(id, true);
        final Set<Integer> set = IntStream.of(ids1).boxed().collect(Collectors.toSet());
        set.retainAll(IntStream.of(ids2).boxed().collect(Collectors.toSet()));
        assertThat(bucket1.countOverlapWith(bucket2), is(set.size()));
    }
    @Test public void testRetainOverlapWith() {
        final BitsBucket bucket1 = new BitsBucket();
        final BitsBucket bucket2 = new BitsBucket();
        final int[] ids1 = { 0, 1, 6, 500, 1234, 2300, 5000, 5300, 7261, 8190 };
        final int[] ids2 = { 0, 2, 5, 500, 1000, 2300, 3000, 4999, 5300 };
        for(final int id : ids1) bucket1.set(id, true);
        for(final int id : ids2) bucket2.set(id, true);

        final Set<Integer> set = IntStream.of(ids1).boxed().collect(Collectors.toSet());
        set.retainAll(IntStream.of(ids2).boxed().collect(Collectors.toSet()));
        final int[] expected = set.stream().mapToInt(i->i).toArray();
        Arrays.sort(expected);

        assertThat(BitsBucket.from(bucket1).retainOverlapWith(bucket2).countSetBits(), is(set.size()));
        assertThat(BitsBucket.from(bucket1).retainOverlapWith(bucket2).getIndices(), is(expected));
    }
    @Test public void testRemoveOverlapWith() {
        final BitsBucket bucket1 = new BitsBucket();
        final BitsBucket bucket2 = new BitsBucket();
        final int[] ids1 = { 0, 1, 6, 500, 1234, 2300, 5000, 5300, 7261, 8190 };
        final int[] ids2 = { 0, 2, 5, 500, 1000, 2300, 3000, 4999, 5300 };
        for(final int id : ids1) bucket1.set(id, true);
        for(final int id : ids2) bucket2.set(id, true);

        final Set<Integer> set = IntStream.of(ids1).boxed().collect(Collectors.toSet());
        set.removeAll(IntStream.of(ids2).boxed().collect(Collectors.toSet()));
        final int[] expected = set.stream().mapToInt(i->i).toArray();
        Arrays.sort(expected);

        assertThat(BitsBucket.from(bucket1).removeOverlapWith(bucket2).countSetBits(), is(set.size()));
        assertThat(BitsBucket.from(bucket1).removeOverlapWith(bucket2).getIndices(), is(expected));
    }
    @Test public void testJoinWith() {
        final BitsBucket bucket1 = new BitsBucket();
        final BitsBucket bucket2 = new BitsBucket();
        final int[] ids1 = {     100, 500,  700,      };
        final int[] ids2 = { 0, 2, 5, 500, 1000, 5300 };
        for(final int id : ids1) bucket1.set(id, true);
        for(final int id : ids2) bucket2.set(id, true);

        final Set<Integer> set = new HashSet<>();
        set.addAll(IntStream.of(ids1).boxed().collect(Collectors.toList()));
        set.addAll(IntStream.of(ids2).boxed().collect(Collectors.toList()));
        final int[] joinedIds = set.stream().mapToInt(i->i).toArray();
        Arrays.sort(joinedIds);

        assertThat(BitsBucket.from(bucket1).joinWith(bucket2).getIndices(), is(joinedIds));
    }

    @Test public void testIsEmpty() {
        final BitsBucket bucket = new BitsBucket();
        assertTrue(bucket.isEmpty());
        bucket.set(0, true);
        assertFalse(bucket.isEmpty());
        bucket.set(0, false);
        assertTrue(bucket.isEmpty());
    }
    @Test public void testDirty() {
        final BitsBucket bucket = new BitsBucket();
        assertFalse(bucket.isDirty());

        bucket.set(0, true);
        assertTrue(bucket.isDirty());

        bucket.clearDirty();
        assertFalse(bucket.isDirty());

        bucket.set(0, true);
        assertFalse(bucket.isDirty());

        bucket.set(1, true);
        assertTrue(bucket.isDirty());

        bucket.clearDirty();
        bucket.retainOverlapWith(bucket);
        assertFalse(bucket.isDirty());

        bucket.retainOverlapWith(new BitsBucket());
        assertTrue(bucket.isDirty());
    }

    @Test public void testReadWrite() {
        final BitsBucket bucket = new BitsBucket();

        for(int i=0; i<bucket.size(); i+=5) bucket.set(i, true);
        assertThat(bucket.countSetBits(), is(bucket.size() / 5 + 1));

        final byte[] data = bucket.toByteArray();
        final BitsBucket bucket2 = BitsBucket.from(data);
        assertThat(bucket2.countSetBits(), is(bucket.size() / 5 + 1));

        final BitsBucket bucketOnes = new BitsBucket();
        for(int i=0; i<bucketOnes.size(); i++) bucketOnes.set(i, true);
        assertThat(bucketOnes.countSetBits(), is(bucketOnes.size()));

        final byte[] dataOnes = bucketOnes.toByteArray();
        assertThat(dataOnes.length, is(Integer.BYTES));
        final BitsBucket bucketOnes2 = BitsBucket.from(dataOnes);
        assertThat(bucketOnes2.countSetBits(), is(bucketOnes.size()));
    }
}