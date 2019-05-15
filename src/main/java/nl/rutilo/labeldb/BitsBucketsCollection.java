package nl.rutilo.labeldb;

import nl.rutilo.labeldb.util.ByteArraySource;
import nl.rutilo.labeldb.util.ByteArrayTarget;

import java.util.Objects;
import java.util.stream.Stream;

class BitsBucketsCollection {
    private BitsBucket[] buckets = new BitsBucket[Bits.COLLECTION_BUCKETS_COUNT];

    public static BitsBucketsCollection from(BitsBucketsCollection other) {
        final BitsBucketsCollection buckets = new BitsBucketsCollection();
        for(int i=0; i<buckets.buckets.length; i++) {
            if(other.buckets[i] != null) {
                buckets.buckets[i] = other.buckets[i].copy();
            }
        }
        return buckets;
    }
    public static BitsBucketsCollection from(byte[] data) {
        final BitsBucketsCollection buckets = new BitsBucketsCollection();
        final ByteArraySource source = new ByteArraySource(data);
        final short bucketCount = source.getShort();
        while(source.hasMoreData()) {
            final short bucketIndex = source.getShort();
            final byte[] bucketData = new byte[source.getShort()];
            source.copyInto(bucketData);
            buckets.buckets[bucketIndex] = BitsBucket.from(bucketData);
        }
        return buckets;
    }

    public BitsBucketsCollection copy() { return from(this); }
    public byte[] toByteArray() {
        compact();
        final byte[][] bucketsData = new byte[buckets.length][];
        for(int i=0; i<buckets.length; i++) if(buckets[i] != null) bucketsData[i] = buckets[i].toByteArray();
        final short bucketCount = (short)Stream.of(bucketsData).filter(Objects::nonNull).count();
        final int byteSize = Stream.of(bucketsData).filter(Objects::nonNull).mapToInt(d->2+2+d.length).sum();

        final ByteArrayTarget target = new ByteArrayTarget(2 + byteSize);
        target.add(bucketCount);
        for(short i=0; i<bucketsData.length; i++) {
            final byte[] data = bucketsData[i];
            if(data != null) {
                target.add(i);
                target.add((short)data.length);
                target.add(data);
            }
        }
        return target.toByteArray();
    }

    public int size() {
        return Bits.COLLECTION_BITS_COUNT;
    }

    public BitsBucketsCollection clear() {
        for(int i=0; i<buckets.length; i++) buckets[i] = null;
        return this;
    }

    public int copyIndicesIn(int[] array, int arrayOffset0, int idOffset0) {
        int arrayOffset = arrayOffset0;
        int idOffset    = idOffset0;
        for (final BitsBucket bucket : buckets) {
            if (bucket != null) {
                final int copied = bucket.copyIndicesIn(array, arrayOffset, idOffset);
                arrayOffset += copied;
            }
            idOffset += Bits.BUCKET_BITS_COUNT;
        }
        return arrayOffset - arrayOffset0;
    }
    public int[] getIndices() {
        final int[] indices = new int[countSetBits()];
        copyIndicesIn(indices, 0, 0);
        return indices;
    }
    public BitsBucketsCollection set(int index, boolean set) {
        if(index < 0 || index >= Bits.COLLECTION_BITS_COUNT) throw new IllegalArgumentException("Index of " + index + " is outside range of 0..COLLECTION_BITS_COUNT (" + Bits.COLLECTION_BITS_COUNT + ")");
        final int bucketIndex = index / Bits.BUCKET_BITS_COUNT;
        getBucket(bucketIndex).set(index % Bits.BUCKET_BITS_COUNT, set);
        return this;
    }
    public boolean isSet(int index) {
        final int bucketIndex = index / Bits.BUCKET_BITS_COUNT;
        if(buckets[bucketIndex] == null) return false;
        return buckets[bucketIndex].isSet(index % Bits.BUCKET_BITS_COUNT);
    }
    public boolean isAnySet() {
        for(final BitsBucket bucket : buckets) {
            if(bucket != null && bucket.isAnySet()) return true;
        }
        return false;
    }
    public boolean isAnySet(int beginIndex, int endIndex) {
        final int maxBitIndex = Math.min(endIndex, buckets.length * Bits.BUCKET_BITS_COUNT - 1);
        for(int bitIndex = beginIndex; bitIndex <= maxBitIndex; bitIndex++) {
            if(bitIndex % Bits.BUCKET_BITS_COUNT == 0 && bitIndex + Bits.BUCKET_BITS_COUNT <= maxBitIndex + 1) {
                final BitsBucket bucket = buckets[bitIndex / Bits.BUCKET_BITS_COUNT];
                if(bucket != null && bucket.isAnySet()) return true;
                bitIndex += Bits.BUCKET_BITS_COUNT - 1;
            } else {
                if(isSet(bitIndex)) return true;
            }
        }
        return false;
    }

    public BitsBucketsCollection reverse() {
        int lastBucket = buckets.length - 1;
        while(lastBucket > 0 && buckets[lastBucket] == null) lastBucket--;
        for(int i=0; i<=lastBucket; i++) {
            final BitsBucket bucket = getBucket(i);
            bucket.reverse();
            if(bucket.isEmpty()) buckets[i] = null;
        }
        return this;
    }

    public int countSetBits() {
        int count = 0;
        for (BitsBucket bucket : buckets) {
            if (bucket != null) count += bucket.countSetBits();
        }
        return count;
    }
    public int countOverlapWith(BitsBucketsCollection other) {
        int count = 0;
        for(int index = 0; index < buckets.length; index++) {
            if(buckets[index] != null) {
                count += buckets[index].countOverlapWith(other.buckets[index]);
            }
        }
        return count;
    }
    public BitsBucketsCollection retainOverlapWith(BitsBucketsCollection other) {
        for(int index = 0; index < buckets.length; index++) {
            if(buckets[index] != null) {
                buckets[index].retainOverlapWith(other.buckets[index]);
            }
        }
        return this;
    }
    public BitsBucketsCollection removeOverlapWith(BitsBucketsCollection other) {
        for(int index = 0; index < buckets.length; index++) {
            if(buckets[index] != null && other.buckets[index] != null) {
                buckets[index].removeOverlapWith(other.buckets[index]);
            }
        }
        return this;
    }
    public BitsBucketsCollection joinWith(BitsBucketsCollection other) {
        for(int index = 0; index < buckets.length; index++) {
            if(buckets[index] == null && other.buckets[index] != null) {
                buckets[index] = other.buckets[index].copy();
            } else
            if(buckets[index] != null) {
                buckets[index].joinWith(other.buckets[index]);
            }
        }
        return this;
    }

    private BitsBucket getBucket(int index) {
        if(index < 0 || index >= buckets.length) throw new IllegalArgumentException("index should be 0..COLLECTION_BUCKETS_COUNT (" + buckets.length + ") but is " + index);
        if(buckets[index] == null) buckets[index] = new BitsBucket();
        return buckets[index];
    }

    public BitsBucketsCollection compact() {
        for(int index = 0; index < buckets.length; index++) {
            if (buckets[index] != null && buckets[index].isEmpty()) buckets[index] = null;
        }
        return this;
    }
    public boolean isEmpty() {
        return Stream.of(buckets).filter(Objects::nonNull).allMatch(BitsBucket::isEmpty);
    }
    public boolean isDirty() {
        return Stream.of(buckets).filter(Objects::nonNull).anyMatch(BitsBucket::isDirty);
    }
    public BitsBucketsCollection clearDirty() {
        Stream.of(buckets).filter(Objects::nonNull).forEach(BitsBucket::clearDirty);
        return this;
    }
}
