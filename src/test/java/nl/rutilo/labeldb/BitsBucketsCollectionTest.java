package nl.rutilo.labeldb;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class BitsBucketsCollectionTest {
    final static int[] testIds = new int[100_000];
    final static int MAX_ID = Bits.COLLECTION_BITS_COUNT - 1;

    @BeforeClass public static void setup() {
        final Random random = new Random(0); // pseudo-random: each test the same numbers
        final Set<Integer> randomIds = new HashSet<>(); // use set to prevent duplicate values
        while(randomIds.size() < testIds.length) randomIds.add(random.nextInt(MAX_ID + 1));
        final int[] randomIdsArray = randomIds.stream().mapToInt(i->i).toArray();
        System.arraycopy(randomIdsArray, 0, testIds, 0, testIds.length);
        Arrays.sort(testIds); // not needed to set, but easier to compare because getIndices() returns sorted
    }

    @Test public void testSize() {
        assertThat(new BitsBucketsCollection().size(), is(Bits.COLLECTION_BITS_COUNT));
    }

    @Test public void testSmallSetOfIds() { // small set is easier to debug
        final BitsBucketsCollection buckets = new BitsBucketsCollection();
        final int[] ids = { 1, 1000, 8000, 12_000, 25_000, 123_456, 250_000 };
        for(final int id : ids) buckets.set(id, true);
        for(final int id : ids) assertThat("" + id, buckets.isSet(id), is(true));

        assertThat(buckets.getIndices(), is(ids));
        assertThat(buckets.countSetBits(), is(ids.length));

        final int[] ids2 = { 1, 2, 3, 4, 12_000, 200_000, 250_000 };
        final BitsBucketsCollection buckets2 = new BitsBucketsCollection();
        for(final int id : ids2) buckets2.set(id, true);

        final Set<Integer> set = IntStream.of(ids).boxed().collect(Collectors.toSet());
        set.retainAll(IntStream.of(ids2).boxed().collect(Collectors.toSet()));

        assertThat(buckets.countOverlapWith(buckets2), is(set.size()));
        buckets.retainOverlapWith(buckets2);
        assertThat(buckets.countSetBits(), is(3));
    }

    @Test public void testClear() {
        final BitsBucketsCollection buckets = new BitsBucketsCollection();
        for(final int id : testIds) buckets.set(id, true);
        assertThat(buckets.countSetBits(), is(testIds.length));
        buckets.clear();
        assertThat(buckets.countSetBits(), is(0));
    }
    @Test public void testSet() {
        final BitsBucketsCollection buckets = new BitsBucketsCollection();
        for(final int id : testIds) buckets.set(id, true);

        assertThat(buckets.getIndices(), is(testIds));
    }
    @Test public void testSetAndIsSet() {
        final BitsBucketsCollection buckets = new BitsBucketsCollection();
        final int testBits = 25_000;

        for(int i=0; i<testBits; i+=50) {
            assertFalse(buckets.isSet(i));
            buckets.set(i, true);
            for(int j=0; j<testBits; j+=50) {
                assertThat(buckets.isSet(j), is(j == i));
            }
            buckets.set(i, false);
            for(int j=0; j<testBits; j+=50) {
                assertFalse(buckets.isSet(j));
            }
        }
        buckets.set(5000, true);
        buckets.set(20_000, true);
        buckets.set(80_000, true);
        for(int j=0; j<=80_000; j++) {
            assertThat(buckets.isSet(j), is(j == 5000 || j == 20_000 || j == 80_000));
        }
        try {
            buckets.set(Bits.COLLECTION_BITS_COUNT + 1, true);
            fail("Expected trow");
        } catch(final IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("is outside range"));
        }
    }
    @Test public void testIsAnySet() {
        final BitsBucketsCollection buckets = new BitsBucketsCollection();
        assertFalse(buckets.isAnySet());
        assertFalse(buckets.isAnySet(0, Bits.COLLECTION_BITS_COUNT));
        for(int index=0; index<Bits.COLLECTION_BITS_COUNT; index+= index < 10_000 ? 1 : 500) {
            buckets.clear();
            buckets.set(index, true);
            assertTrue("failed for full range: " + index, buckets.isAnySet(0, Bits.COLLECTION_BITS_COUNT));
            assertTrue("failed for index,index " + index, buckets.isAnySet(index, index));
            assertTrue("failed for >=from "      + index, buckets.isAnySet(index, Bits.COLLECTION_BITS_COUNT));
            assertTrue("failed for <=from "      + index, buckets.isAnySet(0, index));
            assertFalse("failed for <from "      + index, buckets.isAnySet(0, index-1));
            assertFalse("failed for >from "      + index, buckets.isAnySet(index+1, Bits.COLLECTION_BITS_COUNT));
        }
    }

    @Test public void testReverse() {
        final BitsBucketsCollection buckets = new BitsBucketsCollection();
        assertThat(buckets.countSetBits(), is(0));
        buckets.set(100, false);
        buckets.reverse();
        assertThat(Math.min(100, buckets.countSetBits()), is(100));
        buckets.reverse();
        assertThat(buckets.countSetBits(), is(0));
        buckets.set(1, true);
        buckets.set(10_000, true);
        assertThat(buckets.countSetBits(), is(2));
        buckets.reverse();
        assertThat(buckets.isSet(1), is(false));
        assertThat(buckets.isSet(10_000), is(false));
    }

    @Test public void testCountSetBits() {
        final BitsBucketsCollection buckets = new BitsBucketsCollection();
        for(final int id : testIds) buckets.set(id, true);
        assertThat(buckets.countSetBits(), is(testIds.length));
    }
    @Test public void testCountOverlapWith() {
        final BitsBucketsCollection buckets1 = new BitsBucketsCollection();
        final BitsBucketsCollection buckets2 = new BitsBucketsCollection();
        final int[] ids1 = { 0, 1, 6, 500,       123_456, 330_967, 370_888, 500_000, 500_001, 500_124 };
        final int[] ids2 = { 0, 2, 5, 500, 1000, 123_456, 234_567, 370_888,          500_001, 500_123 };
        for(final int id : ids1) buckets1.set(id, true);
        for(final int id : ids2) buckets2.set(id, true);
        final Set<Integer> set = IntStream.of(ids1).boxed().collect(Collectors.toSet());
        set.retainAll(IntStream.of(ids2).boxed().collect(Collectors.toSet()));
        assertThat(buckets1.countOverlapWith(buckets2), is(set.size()));
    }
    @Test public void testRetainOverlapWith() {
        final BitsBucketsCollection buckets1 = new BitsBucketsCollection();
        final BitsBucketsCollection buckets2 = new BitsBucketsCollection();
        final int[] ids1 = { 0, 1, 6, 500,       123_456, 330_967, 370_888, 500_000, 500_001, 500_124 };
        final int[] ids2 = { 0, 2, 5, 500, 1000, 123_456, 234_567, 370_888,          500_001, 500_123 };
        for(final int id : ids1) buckets1.set(id, true);
        for(final int id : ids2) buckets2.set(id, true);

        final Set<Integer> set = IntStream.of(ids1).boxed().collect(Collectors.toSet());
        set.retainAll(IntStream.of(ids2).boxed().collect(Collectors.toSet()));
        final int[] array = set.stream().mapToInt(i->i).toArray();
        Arrays.sort(array);

        assertThat(BitsBucketsCollection.from(buckets1).retainOverlapWith(buckets2).countSetBits(), is(set.size()));
        assertThat(BitsBucketsCollection.from(buckets1).retainOverlapWith(buckets2).getIndices(), is(array));
    }
    @Test public void testRemoveOverlapWith() {
        final BitsBucketsCollection buckets1 = new BitsBucketsCollection();
        final BitsBucketsCollection buckets2 = new BitsBucketsCollection();
        final int[] ids1 = { 0, 1, 6, 500,       123_456, 330_967, 370_888, 500_000, 500_001, 500_124 };
        final int[] ids2 = { 0, 2, 5, 500, 1000, 123_456, 234_567, 370_888,          500_001, 500_123 };
        for(final int id : ids1) buckets1.set(id, true);
        for(final int id : ids2) buckets2.set(id, true);

        final Set<Integer> set = IntStream.of(ids1).boxed().collect(Collectors.toSet());
        set.removeAll(IntStream.of(ids2).boxed().collect(Collectors.toSet()));
        final int[] array = set.stream().mapToInt(i->i).toArray();
        Arrays.sort(array);

        assertThat(BitsBucketsCollection.from(buckets1).removeOverlapWith(buckets2).countSetBits(), is(set.size()));
        assertThat(BitsBucketsCollection.from(buckets1).removeOverlapWith(buckets2).getIndices(), is(array));
    }
    @Test public void testJoinWith() {
        final BitsBucketsCollection buckets1 = new BitsBucketsCollection();
        final BitsBucketsCollection buckets2 = new BitsBucketsCollection();
        final int[] ids1 = {                     123_456, 330_967, 370_888 };
        final int[] ids2 = { 0, 2, 5, 500, 1000, 123_456, 234_567, 370_888, 500_000 };
        for(final int id : ids1) buckets1.set(id, true);
        for(final int id : ids2) buckets2.set(id, true);

        final Set<Integer> set = new HashSet<>();
        set.addAll(IntStream.of(ids1).boxed().collect(Collectors.toList()));
        set.addAll(IntStream.of(ids2).boxed().collect(Collectors.toList()));
        final int[] joinedIds = set.stream().mapToInt(i->i).toArray();
        Arrays.sort(joinedIds);

        assertThat(BitsBucketsCollection.from(buckets1).joinWith(buckets2).getIndices(), is(joinedIds));
    }

    @Test public void testIsEmpty() {
        final BitsBucketsCollection buckets = new BitsBucketsCollection();
        assertTrue(buckets.isEmpty());
        buckets.set(0, true);
        assertFalse(buckets.isEmpty());
        buckets.set(0, false);
        assertTrue(buckets.isEmpty());
    }
    @Test public void testDirty() {
        final BitsBucketsCollection buckets = new BitsBucketsCollection();
        assertFalse(buckets.isDirty());

        buckets.set(0, true);
        assertTrue(buckets.isDirty());

        buckets.clearDirty();
        assertFalse(buckets.isDirty());

        buckets.set(0, true);
        assertFalse(buckets.isDirty());

        buckets.set(1, true);
        assertTrue(buckets.isDirty());

        buckets.clearDirty();
        buckets.retainOverlapWith(buckets);
        assertFalse(buckets.isDirty());

        buckets.retainOverlapWith(new BitsBucketsCollection());
        assertTrue(buckets.isDirty());
    }

    @Test public void testReadWrite() {
        final BitsBucketsCollection buckets = new BitsBucketsCollection();

        for(final int id : testIds) buckets.set(id, true);
        assertThat(buckets.countSetBits(), is(testIds.length));

        final byte[] data = buckets.toByteArray();
        final BitsBucketsCollection bucketsCopy = BitsBucketsCollection.from(data);
        assertThat(bucketsCopy.countSetBits(), is(testIds.length));
        assertThat(bucketsCopy.getIndices(), is(testIds));

        //System.out.println("size of " + testIds.length + " bits is " + data.length + " bytes");

        //BitsTest.timed("(de)serializing", 10_000, () -> {
        //    BitsBucketsCollection.from(buckets.toByteArray());
        //});
    }
}