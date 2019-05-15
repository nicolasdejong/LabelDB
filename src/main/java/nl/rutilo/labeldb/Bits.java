package nl.rutilo.labeldb;

import nl.rutilo.labeldb.util.ByteArraySource;
import nl.rutilo.labeldb.util.ByteArrayTarget;
import nl.rutilo.labeldb.util.Utils;

import java.io.File;
import java.util.Objects;
import java.util.stream.Stream;

import static nl.rutilo.labeldb.util.Utils.or;

public class Bits {
    protected static final int BUCKET_BYTE_SIZE = 1024; // 1KB
    protected static final int BUCKET_LONG_COUNT = BUCKET_BYTE_SIZE / 8;
    protected static final int BUCKET_BITS_COUNT = BUCKET_BYTE_SIZE * 8; // 8192 bits per bucket at 1KB
    protected static final int COLLECTION_BUCKETS_COUNT = 64; // 64KB -> 524288 bits per collection
    protected static final int COLLECTION_BITS_COUNT = COLLECTION_BUCKETS_COUNT * BUCKET_BITS_COUNT;
    private   static final int INITIAL_COLLECTIONS_COUNT = 10;

    // Each bucket collection is saved in an individual file.
    // This way a partial update of the bits only needs a save
    // of a single (max) ~64KB file, each representing (max) 524288 bits.
    // This is a sparse array (meaning it may contain nulls).
    // It will be enlarged when needed.
    private BitsBucketsCollection[] buckets;
    private int maxIndex = 0;
    private boolean isValid = true;
    public final String name;
    public final File dir;

    public Bits() { this(""); }
    public Bits(String name) {
        this.name = name == null ? "" : name;
        this.dir = null;
        clear();
    }
    public Bits(String name, File dir) {
        this.name = name;
        this.dir = new File(dir, Utils.nameToFilename(name));
        clear();
        load();
    }

    public static Bits of(String name) { return new Bits(name); }
    public static Bits of(String name, File dir) { return new Bits(name, dir); }
    public static Bits of(Bits other) {
        final Bits copy = new Bits(other.name);
        copy.buckets = new BitsBucketsCollection[other.buckets.length];
        for(int i=0; i<copy.buckets.length; i++) {
            if(other.buckets[i] != null) copy.buckets[i] = other.buckets[i].copy();
        }
        copy.maxIndex = other.maxIndex;
        return copy;
    }

    public boolean isValid() { return isValid; }
    public Bits copy() { return Bits.of(this); }

    public final Bits clear() {
        buckets = new BitsBucketsCollection[INITIAL_COLLECTIONS_COUNT];
        isValid = true;
        maxIndex = 0;
        return this;
    }

    public Bits setRange(int from, int upto) {
        for(int index=from; index<=upto; index++)  set(index, true);
        return this;
    }
    public Bits set(int... indices) {
        for(final int index : indices) set(index, true);
        return this;
    }
    public Bits unset(int... indices) {
        for(final int index : indices) set(index, false);
        return this;
    }
    public Bits set(int index, boolean set) {
        final int bucketsIndex = index / COLLECTION_BITS_COUNT;
        final BitsBucketsCollection buckets = getBuckets(bucketsIndex);
        buckets.set(index % COLLECTION_BITS_COUNT, set);
        if(index > maxIndex) maxIndex = index;
        return this;
    }

    public boolean isSet(int index) {
        if(index > maxIndex) return false;
        final int bucketsIndex = index / COLLECTION_BITS_COUNT;
        final BitsBucketsCollection buckets = getBuckets(bucketsIndex);
        return buckets.isSet(index % COLLECTION_BITS_COUNT);
    }
    public boolean isAnySet() { return isAnySet(0, buckets.length * COLLECTION_BITS_COUNT); }
    public boolean isAnySet(int fromIndex, int uptoIndex) {
        int maxBitIndex = Math.min(uptoIndex, maxIndex);
        for(int bitIndex = fromIndex; bitIndex <= maxBitIndex; bitIndex++) {
            if(bitIndex % COLLECTION_BITS_COUNT == 0 && bitIndex + COLLECTION_BITS_COUNT <= uptoIndex) {
                final int bucketsIndex = bitIndex / COLLECTION_BITS_COUNT;
                final BitsBucketsCollection buckets = getBuckets(bucketsIndex);
                if(buckets.isAnySet()) return true;
                bitIndex += COLLECTION_BITS_COUNT -1;
            } else {
                if(isSet(bitIndex)) return true;
            }
        }
        return false;
    }

    public int[] getIndices() {
        final int[] indices = new int[countSetBits()];
        int idOffset = 0;
        int offset = 0;
        for (final BitsBucketsCollection bbCollection : buckets) {
            if (bbCollection != null) {
                offset += bbCollection.copyIndicesIn(indices, offset, idOffset);
            }
            idOffset += Bits.COLLECTION_BITS_COUNT;
        }
        return indices;
    }

    public Bits reverse() {
        int lastBucket = buckets.length - 1;
        while(lastBucket > 0 && buckets[lastBucket] == null) lastBucket--;
        for(int i=0; i<=lastBucket; i++) {
            final BitsBucketsCollection bcol = getBuckets(i);
            bcol.reverse();
            if(bcol.isEmpty()) buckets[i] = null;
        }
        final int maxBucketIndex = (lastBucket + 1) * COLLECTION_BITS_COUNT - 1;
        for(int i=maxIndex + 1; i <= maxBucketIndex; i++) set(i, false);
        return this;
    }

    public int countSetBits() {
        int count = 0;
        for (BitsBucketsCollection bbCollection : buckets) {
            if (bbCollection != null) count += bbCollection.countSetBits();
        }
        return count;
    }
    public int countOverlapWith(Bits other) {
        int count = 0;
        final int length = Math.min(buckets.length, other.buckets.length);
        for(int index = 0; index < length; index++) {
            if(buckets[index] != null && other.buckets[index] != null) {
                count += buckets[index].countOverlapWith(other.buckets[index]);
            }
        }
        return count;
    }
    public Bits retainOverlapWith(Bits other) {
        for(int index = 0; index < buckets.length; index++) {
            if(buckets[index] != null) {
                if(index >= other.buckets.length || other.buckets[index] == null) {
                    buckets[index] = null;
                } else {
                    buckets[index].retainOverlapWith(other.buckets[index]);
                }
            }
        }
        return this;
    }
    public Bits removeOverlapWith(Bits other) {
        for(int index = 0; index < buckets.length; index++) {
            if(buckets[index] != null && index < other.buckets.length && other.buckets[index] != null) {
                buckets[index].removeOverlapWith(other.buckets[index]);
            }
        }
        return this;
    }
    public Bits joinWith(Bits other) {
        for(int index = 0; index < other.buckets.length; index++) {
            if(other.buckets[index] != null) {
                if (index >= buckets.length || buckets[index] == null) {
                    getBuckets(index);
                    buckets[index] = other.buckets[index].copy();
                } else
                if (other.buckets[index] != null) {
                    buckets[index].joinWith(other.buckets[index]);
                }
            }
        }
        return this;
    }

    public boolean isDirty() {
        return Stream.of(buckets).filter(Objects::nonNull).anyMatch(BitsBucketsCollection::isDirty);
    }
    public Bits store() {
        if(this.dir == null || !isDirty() || name.isEmpty()) return this;
        compact();
        new ByteArrayTarget()
            .add(countSetBits())
            .add((short)buckets.length)
            .add(name)
            .writeTo(new File(dir, "Bits"));

        Stream.of(buckets)
            .filter(Objects::nonNull)
            .filter(BitsBucketsCollection::isDirty)
            .forEach(bbColl -> {
                Utils.writeByteArrayToFile(
                    new File(dir, "" + indexOf(bbColl)), bbColl.toByteArray());
                bbColl.clearDirty();
            });
        return this;
    }
    public Bits load() {
        if(this.dir == null || !this.dir.exists() || name.isEmpty()) return this;
        clear();
        final ByteArraySource validateData = new ByteArraySource(new File(dir, "Bits"));
        final int expectedSetBitsCount = validateData.getInt();
        final int expectedBucketLength = validateData.getShort();
        if(!name.equals(validateData.getString())) { isValid = false; return this; }

        Stream.of(or(this.dir.listFiles(), new File[0]))
            .filter(file -> file.getName().matches("^\\d+$"))
            .forEach(file -> {
                final int index = Integer.parseInt(file.getName());
                makeRoomForBuckets(index);

                final byte[] data = Utils.readFileToByteArray(file).orElse(new byte[0]);
                buckets[index] = BitsBucketsCollection.from(data);
                maxIndex = Math.max(maxIndex, index * BUCKET_BITS_COUNT);
            });
        isValid = countSetBits() == expectedSetBitsCount
               && buckets.length == expectedBucketLength;
        if(!isValid) clear();
        return this;
    }

    private Bits compact() {
        for(int index = 0; index < buckets.length; index++) {
            if (buckets[index] != null && buckets[index].compact().isEmpty()) buckets[index] = null;
        }
        return this;
    }
    private BitsBucketsCollection getBuckets(int index) {
        makeRoomForBuckets(index);
        if(buckets[index] == null) buckets[index] = new BitsBucketsCollection();
        return buckets[index];
    }
    private void makeRoomForBuckets(int index) {
        if(index >= buckets.length) {
            final BitsBucketsCollection[] newCollections = new BitsBucketsCollection[index + 10];
            System.arraycopy(buckets, 0, newCollections, 0, buckets.length);
            buckets = newCollections;
        }
    }
    private int indexOf(BitsBucketsCollection bbColl) {
        int index = -1;
        for(int i=0; i<buckets.length && index < 0; i++) if(bbColl == buckets[i]) index = i;
        return index;
    }

}
