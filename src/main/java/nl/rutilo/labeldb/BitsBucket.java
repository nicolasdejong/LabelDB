package nl.rutilo.labeldb;

import nl.rutilo.labeldb.util.ByteArraySource;
import nl.rutilo.labeldb.util.ByteArrayTarget;

class BitsBucket {
    // Performance is important here, so to keep things fast,
    // no Collections or objects are used (so also no streams).
    private final long[] longs = new long[Bits.BUCKET_LONG_COUNT];
    private boolean isDirty;
    private int maxUsedLongIndex;
    private int minUsedLongIndex;

    public byte[] toByteArray() {
        if(countSetBits() == size()) {
            return new ByteArrayTarget(Integer.BYTES).add(-1).toByteArray();
        }
        final int longCount = maxUsedLongIndex - minUsedLongIndex + 1;

        return new ByteArrayTarget(new byte[2 * Integer.BYTES + longCount * Long.BYTES])
            .add(longCount)
            .add(minUsedLongIndex)
            .add(longs, minUsedLongIndex, longCount)
            .toByteArray();
    }
    public static BitsBucket from(byte[] data) {
        final BitsBucket bucket = new BitsBucket();
        final ByteArraySource source = new ByteArraySource(data);
        final int longCount = source.getInt();
        if(longCount == -1) {
            for(int i=0; i<bucket.longs.length; i++) bucket.longs[i] = -1;
        } else {
            if(longCount < 0 || longCount > bucket.longs.length) throw new IllegalStateException("Mangled data (longCount=" + longCount + ")");
            bucket.minUsedLongIndex = source.getInt();
            source.getLongs(bucket.longs, bucket.minUsedLongIndex, longCount);
        }
        bucket.resetMinMaxIndex();
        return bucket;
    }
    public static BitsBucket from(BitsBucket toCopy) {
        final BitsBucket bucket = new BitsBucket();
        System.arraycopy(toCopy.longs, 0, bucket.longs, 0, bucket.longs.length);
        bucket.isDirty = false;
        bucket.minUsedLongIndex = toCopy.minUsedLongIndex;
        bucket.maxUsedLongIndex = toCopy.maxUsedLongIndex;
        return bucket;
    }
    public BitsBucket copy() { return from(this); }

    public boolean clear() {
        boolean changed = false;
        for(int i=0; i<longs.length; i++) if(longs[i] != 0) { changed = true; longs[i] = 0; }
        isDirty |= changed;
        return changed;
    }
    /** Number of bits in this bucket that can be set */
    public int size() {
        return longs.length * Long.SIZE;
    }
    public int copyIndicesIn(int[] array, int arrayOffset, int idOffset) {
        int idIndex = 0;
        for(int longIndex = minUsedLongIndex; longIndex <= maxUsedLongIndex; longIndex++) {
            if(longs[longIndex] != 0) {
                final long l = longs[longIndex];
                final int bucketIdOffset = longIndex * 64;
                for(int bitIndex=0; bitIndex<64; bitIndex++) {
                    if((l & (1L << bitIndex)) != 0) array[arrayOffset + idIndex++] = idOffset + bucketIdOffset + bitIndex;
                }
            }
        }
        return idIndex;
    }
    public int[] getIndices() {
        final int[] array = new int[countSetBits()];
        copyIndicesIn(array, 0, 0);
        return array;
    }

    public boolean isSet(int bitIndex) {
        final long bits = longs[bitIndex / 64];
        return bits != 0L && (bits & 1L << (bitIndex % 64)) != 0L;
    }
    public BitsBucket set(int bitIndex, boolean set) {
        final int longIndex = bitIndex / 64;
        if(longIndex < 0 || longIndex >= longs.length) throw new IllegalArgumentException("Index outside bucket (0.." + (size()-1) + ") requested: " + bitIndex);
        long longVal = longs[longIndex];
        long oldVal = longVal;
        if(set) longVal |=   1L << (bitIndex % 64);
        else    longVal &= ~(1L << (bitIndex % 64));

        if(longVal != oldVal) {
            longs[longIndex] = longVal;
            isDirty = true;
            if(longIndex < minUsedLongIndex) minUsedLongIndex = longIndex;
            if(longIndex > maxUsedLongIndex) maxUsedLongIndex = longIndex;
        }
        return this;
    }
    public boolean isAnySet() {
        for(final long l : longs) if(l != 0) return true;
        return false;
    }
    public boolean isAnySet(int fromBitIndex, int uptoBitIndex) {
        final int maxBitIndex = Math.min(uptoBitIndex, Bits.BUCKET_BITS_COUNT - 1);
        for(int bitIndex = fromBitIndex; bitIndex <= maxBitIndex; bitIndex++) {
            if(bitIndex % Long.SIZE == 0 && bitIndex + Long.SIZE <= maxBitIndex + 1) {
                if(longs[bitIndex / Long.SIZE] != 0) return true;
                bitIndex += Long.SIZE - 1;
            } else {
                if(isSet(bitIndex)) return true;
            }
        }
        return false;
    }

    public BitsBucket reverse() {
        minUsedLongIndex = 0;
        maxUsedLongIndex = longs.length - 1;
        for(int i=0; i<longs.length; i++) longs[i] = ~longs[i];
        constrictMinMaxIndex();
        isDirty = true;
        return this;
    }

    public int countSetBits() {
        int count = 0;
        for(int i=minUsedLongIndex; i<=maxUsedLongIndex; i++) {
            if(longs[i] != 0) count += Long.bitCount(longs[i]);
        }
        return count;
    }

    private enum IterateType { COUNT_OVERLAP, RETAIN_OVERLAP, REMOVE_OVERLAP, JOIN }

    private int iterate(IterateType type, BitsBucket other) {
        int count = 0;
        final int minIndex = Math.min(minUsedLongIndex, other.minUsedLongIndex);
        final int maxIndex = type == IterateType.COUNT_OVERLAP ? Math.min(maxUsedLongIndex, other.maxUsedLongIndex)
                                                               : Math.max(maxUsedLongIndex, other.maxUsedLongIndex);
        for (int index = minIndex; index <= maxIndex; index++) {
            switch(type) {
                case COUNT_OVERLAP: {
                    final long overlap = longs[index] & other.longs[index];
                    if (overlap != 0) count += Long.bitCount(overlap);
                    break;
                }
                case RETAIN_OVERLAP: {
                    final long longVal = longs[index];
                    if (longVal != 0) {
                        longs[index] &= other.longs[index];
                        isDirty |= longs[index] != longVal;
                    }
                    break;
                }
                case REMOVE_OVERLAP: {
                    final long longVal = longs[index];
                    final long otherVal = other.longs[index];
                    if (longVal != 0 && otherVal != 0) {
                        longs[index] = longVal & ~otherVal;
                        isDirty |= longs[index] != longVal;
                    }
                    break;
                }
                case JOIN: {
                    final long longVal  = longs[index];
                    final long otherVal = other.longs[index];
                    if (otherVal != 0) {
                        longs[index] |= otherVal;
                        isDirty |= longs[index] != longVal;
                    }
                    break;
                }
            }
        }
        return count;
    }

    public int countOverlapWith(BitsBucket other) {
        int count = 0;
        if(other != null) {
            count = iterate(IterateType.COUNT_OVERLAP, other);
        }
        return count;
    }
    public BitsBucket retainOverlapWith(BitsBucket other) {
        if(other == null) {
            isDirty |= clear();
        } else {
            iterate(IterateType.RETAIN_OVERLAP, other);
            constrictMinMaxIndex();
        }
        return this;
    }
    public BitsBucket removeOverlapWith(BitsBucket other) {
        if(other != null) {
            iterate(IterateType.REMOVE_OVERLAP, other);
            constrictMinMaxIndex();
        }
        return this;
    }
    public BitsBucket joinWith(BitsBucket other) {
        if(other != null) {
            iterate(IterateType.JOIN, other);
            resetMinMaxIndex();
        }
        return this;
    }

    public boolean isDirty() { return isDirty; }
    public BitsBucket clearDirty() { isDirty = false; return this; }
    public boolean isEmpty() { return countSetBits() == 0; }

    private void resetMinMaxIndex() {
        minUsedLongIndex = 0;
        maxUsedLongIndex = longs.length -1;
        constrictMinMaxIndex();
    }
    private void constrictMinMaxIndex() {
        while(minUsedLongIndex < longs.length - 1 && longs[minUsedLongIndex] == 0) minUsedLongIndex++;
        while(maxUsedLongIndex > 0                && longs[maxUsedLongIndex] == 0) maxUsedLongIndex--;
    }
}
