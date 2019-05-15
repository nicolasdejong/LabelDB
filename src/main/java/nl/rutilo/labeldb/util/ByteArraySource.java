package nl.rutilo.labeldb.util;

import java.io.File;
import java.util.Objects;

public class ByteArraySource {
    protected final byte[] data;
    protected int offset;

    public ByteArraySource(byte[] data) {
        this.data = Objects.requireNonNull(data);
        this.offset = 0;
    }
    public ByteArraySource(File file) {
        this(Utils.readFileToByteArray(file).orElse(new byte[0]));
    }

    public boolean hasMoreData() { return data != null && offset < data.length; }

    public long getLong() {
        if(data == null || offset > data.length - 8) return -1;
        long l = 0;
        for(int h=0; h<8; h++) l |= ((long)data[offset++] & 0xFF) << ((7-h) * 8);
        return l;
    }
    public int getInt() {
        if(data == null || offset > data.length - 4) return -1;
        int i = 0;
        for(int h=0; h<4; h++) i |= ((long)data[offset++] & 0xFF) << ((3-h) * 8);
        return i;
    }
    public short getShort() {
        if(data == null || offset > data.length - 2) return -1;
        short s = 0;
        for(int h=0; h<2; h++) s |= ((long)data[offset++] & 0xFF) << ((1-h) * 8);
        return s;
    }

    public long[] getLongs(long[] array, int arrayOffset, int count0) {
        final int count = Math.min(count0, array.length - arrayOffset);
        for(int i=0; i<count; i++) array[arrayOffset + i] = getLong();
        return array;
    }
    public long[] getLongs(long[] array) { return getLongs(array, 0, array.length); }
    public int[] getInts(int[] array, int arrayOffset, int count0) {
        final int count = Math.min(count0, array.length - arrayOffset);
        for(int i=0; i<count; i++) array[arrayOffset + i] = getInt();
        return array;
    }
    public int[] getInts(int[] array) { return getInts(array, 0, array.length); }
    public short[] getShorts(short[] array, int arrayOffset, int count0) {
        final int count = Math.min(count0, array.length - arrayOffset);
        for(int i=0; i<count; i++) array[arrayOffset + i] = getShort();
        return array;
    }
    public short[] getShorts(short[] array) { return getShorts(array, 0, array.length); }

    public byte[] copyInto(byte[] target) {
        if(data != null) {
            System.arraycopy(data, offset, target, 0, target.length);
            offset += target.length;
        }
        return target;
    }
    public byte[] copyBytes(int length) {
        return copyInto(new byte[length]);
    }
    public String getString() {
        return new String(copyBytes(getShort()));
    }
}
