package nl.rutilo.labeldb;

import nl.rutilo.labeldb.util.ByteArraySource;
import nl.rutilo.labeldb.util.ByteArrayTarget;
import nl.rutilo.labeldb.util.Utils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

public class Longs {
    public static final int SAVE_BLOCK_SIZE_KB = 4; // Minimal save block size.
    public static final int DEFAULT_GROWTH_RATE = 1000;
    public static final long VALUE_UNUSED = Long.MIN_VALUE;
    private final String name;
    private long[] longs;
    private int growthRate = DEFAULT_GROWTH_RATE;
    private File file;
    private boolean isDirty;
    private Bits dirtyLongs;

    public Longs() {
        this("");
    }
    public Longs(String name) {
        this(name, DEFAULT_GROWTH_RATE);
    }
    public Longs(String name, int size) {
        this(name, new long[size]);
        for(int i=0; i<size; i++) longs[i] = VALUE_UNUSED;
    }
    public Longs(String name, long[] array) {
        this.name = name == null ? "" : name;
        longs = array;
        isDirty = false;
        dirtyLongs = new Bits();
    }
    public Longs(String name, File dir) {
        this(name, new long[0]);
        file = new File(dir, this.name.isEmpty() ? "" : Utils.nameToFilename(this.name));
        load();
    }

    public Longs setGrowthRate(int gr) {
        growthRate = gr <= 0 ? DEFAULT_GROWTH_RATE : gr;
        return this;
    }

    public Longs clear() {
        longs = new long[0];
        isDirty = true;
        dirtyLongs.clear();
        if(file != null) file.delete();
        return this;
    }

    public long get(int index) {
        if(index < 0 || index >= longs.length) return VALUE_UNUSED;
        return longs[index];
    }
    public Longs set(int index, long value) {
        makeSureIndexExists(index);
        if(longs[index] != value) {
            longs[index] = value;
            dirtyLongs.set(index);
            isDirty = true;
        }
        return this;
    }
    public Longs unset(int... indices) {
        for(final int index : indices) {
            if (index >= 0 && index < longs.length) longs[index] = VALUE_UNUSED;
        }
        return this;
    }
    public boolean isSet(int index) { return index >= 0 && index < longs.length && longs[index] != VALUE_UNUSED; }
    public int getFirstUnsetIndex() {
        for(int index=0; index<longs.length; index++) {
            if (longs[index] == VALUE_UNUSED) return index;
        }
        return longs.length;
    }

    public Bits asBits() { return asBits(Long.MIN_VALUE, Long.MAX_VALUE); }
    public Bits asBits(long minValue, long maxValue) {
        final Bits bits = Bits.of("longs");
        for(int index=0; index<longs.length; index++) {
            final long value = longs[index];
            if(value != VALUE_UNUSED && value >= minValue && value <= maxValue) bits.set(index);
        }
        return bits;
    }

    public boolean isDirty() {
        return isDirty;
    }
    public Longs store() {
        if(!isDirty) return this;
        if(name.isEmpty()) throw new IllegalStateException("Longs needs to have a name to store");
        final int[][] indexRanges = getIndexRangesToStore();
        try(final RandomAccessFile raf = new RandomAccessFile(file, "rws")) {
            if(file.length() > 0 && file.length() < longs.length * Long.BYTES) {
                raf.setLength(longs.length * Long.BYTES);
            }
            for(final int[] indexRange : indexRanges) {
                final int startIndex = indexRange[0];
                final int indexCount = indexRange[1];
                final ByteArrayTarget bat = new ByteArrayTarget(indexCount * Long.BYTES);
                for(int index=startIndex; index<startIndex + indexCount; index++) bat.add(longs[index]);

                raf.seek(startIndex * Long.BYTES);
                raf.write(bat.toByteArray());
            }
        } catch(final IOException cause) {
            throw new RuntimeException("Unable to save longs", cause);
        }
        isDirty = false;
        dirtyLongs.clear();
        return this;
    }
    public void load() {
        final byte[] data = Utils.readFileToByteArray(file).orElse(new byte[0]);
        longs = new long[data.length / Long.BYTES];
        final ByteArraySource bas = new ByteArraySource(data);
        for(int i=0; i<longs.length; i++) longs[i] = bas.getLong();
        isDirty = false;
        dirtyLongs.clear();
    }

    protected int[][] getIndexRangesToStore() {
        final int indexBlockSize = (SAVE_BLOCK_SIZE_KB * 1024) / Long.BYTES;
        final List<int[]> indexRanges = new ArrayList<>();
        int[] currentIndexRange = { 0, -1 }; // start, length

        for(int startIndex = 0; startIndex < longs.length; startIndex += indexBlockSize) {
            if(dirtyLongs.isAnySet(startIndex, startIndex + indexBlockSize - 1)) {
                if(startIndex == currentIndexRange[0] + currentIndexRange[1]) {
                    currentIndexRange[1] += indexBlockSize;
                } else {
                    currentIndexRange = new int[] { startIndex, indexBlockSize };
                    indexRanges.add(currentIndexRange);
                }
            }
        }
        currentIndexRange[1] = Math.min(currentIndexRange[1], longs.length - currentIndexRange[0]);
        final int[][] result = new int[indexRanges.size()][];
        for(int i=0; i<indexRanges.size(); i++) result[i] = indexRanges.get(i);
        return result;
    }

    private void makeSureIndexExists(int index) {
        if(index >= longs.length) {
            final long[] newLongs = new long[index + growthRate];
            System.arraycopy(longs, 0, newLongs, 0, longs.length);
            for(int i=longs.length; i<newLongs.length; i++) newLongs[i] = VALUE_UNUSED;
            longs = newLongs;
        }
    }
}
