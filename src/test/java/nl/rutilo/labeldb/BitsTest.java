package nl.rutilo.labeldb;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static nl.rutilo.util.testsupport.TestUtils.*;
import static nl.rutilo.util.testsupport.TestUtils.createTempDir;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class BitsTest {
    final static int[] testIds = new int[1_000_000];
    final static int[] otherIds = new int[10_000];
    final static int MAX_ID = Bits.COLLECTION_BITS_COUNT * 10; // 5.5 million
    final static int GAP_START = 500_000;
    final static int GAP_END = 1100_000;
    final static Random random = new Random(0); // pseudo-random: each test the same numbers

    @BeforeClass
    public static void setup() {
        System.arraycopy(getRandomIdsOfLength(testIds.length), 0, testIds, 0, testIds.length);
        System.arraycopy(getRandomIdsOfLength(otherIds.length), 0, otherIds, 0, otherIds.length);
        otherIds[0] = testIds[0]; // make sure there is at least an overlap of 1
        Arrays.sort(testIds); // not needed to set, but easier to compare because getIndices() returns sorted
        Arrays.sort(otherIds);
    }
    private static int[] getRandomIdsOfLength(int length) {
        final Set<Integer> randomIds = new HashSet<>(); // use set to prevent duplicate values
        while(randomIds.size() < length) {
            final int rval = random.nextInt(MAX_ID + 1); // random ids with gap 500k..1100k (2nd collection)
            if(rval < GAP_START || rval > GAP_END) randomIds.add(rval);
        }
        return randomIds.stream().mapToInt(i->i).toArray();
    }

    @Test public void testNoName() {
        final Bits bits = new Bits();
        assertThat(bits.name, is(""));
    }
    @Test public void testSparseIds() {
        final int[] ids = { 0, 20_000_000 };
        final Bits bits = Bits.of("sparse").set(ids);
        assertThat(bits.getIndices(), is(ids));
    }
    @Test public void testGetSet() {
        final Bits bits = new Bits("get&set").set(testIds);
        for(final int id : testIds) assertTrue(bits.isSet(id));
        IntStream.range(GAP_START, GAP_END).forEach(id -> assertFalse(bits.isSet(GAP_START)));

        bits.set(0);
        assertTrue(bits.isSet(0));
        bits.unset(0);
        assertFalse(bits.isSet(0));
    }
    @Test public void testIsAnySet() {
        final Bits bits = new Bits("testAnyTest");
        assertFalse(bits.isAnySet());
        assertFalse(bits.isAnySet(0, Bits.COLLECTION_BITS_COUNT));
        final int maxIndex = 2*Bits.COLLECTION_BITS_COUNT;
        for(int index = 0; index <= maxIndex; index+= index < 100 || (index>524000 && index < 525000) ? 1 : 23456) {
            bits.clear();
            bits.set(index, true);
            assertTrue("failed for full range: " + index, bits.isAnySet(0, maxIndex));
            assertTrue("failed for index,index " + index, bits.isAnySet(index, index));
            assertTrue("failed for >=from "      + index, bits.isAnySet(index, maxIndex));
            assertTrue("failed for <=from "      + index, bits.isAnySet(0, index));
            assertFalse("failed for <from "      + index, bits.isAnySet(0, index-1));
            assertFalse("failed for >from "      + index, bits.isAnySet(index+1, maxIndex));
        }
    }
    @Test public void testGetIndices() {
        final Bits bits = new Bits("indices").set(testIds);
        assertThat(bits.getIndices(), is(testIds));
    }
    @Test public void testReverse() {
        final Bits bits = new Bits();
        assertThat(bits.countSetBits(), is(0));
        final int[] sets = new int[] { 1, 10_000, 100_000, 500_000 };
        bits.set(sets);
        for(final int set : sets) assertThat(bits.isSet(set), is(true));
        assertThat(bits.countSetBits(), is(sets.length));
        bits.reverse();
        for(final int set : sets) assertThat(bits.isSet(set), is(false));
        assertThat(bits.isSet(5), is(true));
        bits.reverse();
        assertThat(bits.isSet(5), is(false));
    }
    @Test public void testCountSetBits() {
        final Bits bits = new Bits("countSetBits").set(testIds);
        assertThat(bits.countSetBits(), is(testIds.length));
    }
    @Test public void testCountOverlapWith() {
        final Bits bits = new Bits("countOverlapWith").set(testIds);
        final Bits bits2 = new Bits("overlapping").set(otherIds);

        final Set<Integer> testIdsSet = new HashSet<>();
        int expectedOverlappingCount = 0;
        for(int id : testIds) testIdsSet.add(id);
        for(int id : otherIds) if(testIdsSet.contains(id)) expectedOverlappingCount++;

        assertThat(bits.countOverlapWith(bits2), is(expectedOverlappingCount));
    }
    @Test public void testRetainOverlapWith() {
        final Bits testBits = new Bits("testIds").set(testIds);
        final Bits otherBits = new Bits("toRetain").set(otherIds);
        otherBits.setRange(500_000, 1100_000);

        final Set<Integer> idsSet = new HashSet<>();
        final List<Integer> overlappingIdsList = new ArrayList<>();
        for(int id : testIds) idsSet.add(id);
        for(int id : otherIds) if(idsSet.contains(id)) overlappingIdsList.add(id);
        final int[] expectedIds = overlappingIdsList.stream().mapToInt(i->i).toArray();
        Arrays.sort(expectedIds);

        assertThat(testBits.copy().retainOverlapWith(otherBits).getIndices(), is(expectedIds));
        assertThat(otherBits.copy().retainOverlapWith(testBits).getIndices(), is(expectedIds));
    }
    @Test public void testRemoveOverlapWith() {
        final Bits testBits  = new Bits("testIds").set(testIds);
        final Bits otherBits = new Bits("toRemove").set(otherIds);
        otherBits.setRange(500_000, 1100_000);

        final Set<Integer> remainingIdsSet = new HashSet<>();
        for(int id :  testIds) remainingIdsSet.add(id);
        for(int id : otherBits.getIndices()) remainingIdsSet.remove(id);
        int[] expectedIds = remainingIdsSet.stream().mapToInt(i->i).toArray();
        Arrays.sort(expectedIds);

        assertThat(testBits.copy().removeOverlapWith(otherBits).countSetBits(), is(expectedIds.length));
        assertThat(testBits.copy().removeOverlapWith(otherBits).getIndices(), is(expectedIds));

        remainingIdsSet.clear();
        for(int id : otherBits.getIndices()) remainingIdsSet.add(id);
        for(int id : testIds) remainingIdsSet.remove(id);
        expectedIds = remainingIdsSet.stream().mapToInt(i->i).toArray();
        Arrays.sort(expectedIds);

        assertThat(otherBits.copy().removeOverlapWith(testBits).countSetBits(), is(expectedIds.length));
        assertThat(otherBits.copy().removeOverlapWith(testBits).getIndices(), is(expectedIds));
    }
    @Test public void testJoinWith() {
        final Bits bits = new Bits("countOverlapWith").set(testIds);
        final Bits bits2 = new Bits("overlapping").set(otherIds);

        final Set<Integer> set = new HashSet<>();
        set.addAll(IntStream.of(testIds).boxed().collect(Collectors.toList()));
        set.addAll(IntStream.of(otherIds).boxed().collect(Collectors.toList()));
        final int[] joinedIds = set.stream().mapToInt(i->i).toArray();
        Arrays.sort(joinedIds);

        assertThat(bits.copy().joinWith(bits2).getIndices(), is(joinedIds));
        assertThat(bits2.copy().joinWith(bits).getIndices(), is(joinedIds));

        final int[] testIdsBegin = new int[] { testIds[0], testIds[1], testIds[2] };
        final Bits bits3 = new Bits("overlapping.3").set(testIdsBegin);

        assertThat(bits.copy().joinWith(bits3).getIndices(), is(testIds));
        assertThat(bits3.copy().joinWith(bits).getIndices(), is(testIds));
    }

    @Test public void testSerialization() {
        final String name = "test-serialization";
        try(final DeletedWhenClosedFile dir = createTempDir()) {
            final Bits bits = new Bits(name, dir).set(testIds);
            bits.store();
            final Bits bits2 = Bits.of(name, dir);
            assertTrue(bits2.isValid());
            assertThat(bits2.countSetBits(), is(bits.countSetBits()));
        }
    }

    @Test public void testPerformance() {
//        final int dbSize = 1_000_000;
//        final int cmpCount = 1_000;
//
//        final int[] ids1 = getRandomIdsOfLength(dbSize); // ids between 0 and 5_500_00
//        final int[] ids2 = getRandomIdsOfLength(dbSize); // so one in five bits set
//        final Bits bits1 = new Bits("ids1").set(ids1);      // in actual use there will be large
//        final Bits bits2 = new Bits("ids2").set(ids2);      // gaps, meaning (much) faster operations
//
//        System.out.println("Simulating " + cmpCount + " operations on database of one million elements (worst case).\n");
//
//        final long time = timed(() -> {
//            final long base = timed("countBits    ", cmpCount, bits1::countSetBits);
//            final long maxTime = base * 20;
//            assertTrue(maxTime > timed("countOverlap ", cmpCount, () -> bits1.countOverlapWith(bits2)));
//            assertTrue(maxTime > timed("retainOverlap", cmpCount, () -> bits1.copy().retainOverlapWith(bits2)));
//            assertTrue(maxTime > timed("removeOverlap", cmpCount, () -> bits1.copy().removeOverlapWith(bits2)));
//            assertTrue(maxTime > timed("joinWith     ", cmpCount, () -> bits1.joinWith(bits2)));
//        });
//        System.out.println("Total time: " + time + "ms");
    }
}