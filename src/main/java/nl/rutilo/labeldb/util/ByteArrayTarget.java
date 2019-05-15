package nl.rutilo.labeldb.util;

import java.io.ByteArrayOutputStream;
import java.io.File;

/** Giving an array (size) at construction is about 4 times faster than using the ByteArrayOutputStream */
public class ByteArrayTarget {
    private final byte[] data;
    private int offset;
    private final ByteArrayOutputStream dataStream;

    public ByteArrayTarget() {
        data = null;
        dataStream = new ByteArrayOutputStream(1024);
    }
    public ByteArrayTarget(byte[] data) {
        this.data = data;
        this.offset = 0;
        this.dataStream = null;
    }
    public ByteArrayTarget(int size) { this(new byte[size]); }

    public ByteArrayTarget add(long l) {
        if(dataStream != null) {
            for(int h=0; h<8; h++) dataStream.write((byte)(l >> ((7-h) * 8) & 0xFF));
        } else if(data != null) {
            for(int h=0; h<8; h++) data[offset++] = (byte)(l >> ((7-h) * 8) & 0xFF);
        }
        return this;
    }
    public ByteArrayTarget add(int i) {
        if(dataStream != null) {
            for(int h=0; h<4; h++) dataStream.write((byte)(i >> ((3-h) * 8) & 0xFF));
        } else if(data != null) {
            for(int h=0; h<4; h++) data[offset++] = (byte)(i >> ((3-h) * 8) & 0xFF);
        }
        return this;
    }
    public ByteArrayTarget add(short s) {
        if(dataStream != null) {
            for(int h=0; h<2; h++) dataStream.write((byte)(s >> ((1-h) * 8) & 0xFF));
        } else if(data != null) {
            for(int h=0; h<2; h++) data[offset++] = (byte)(s >> ((1-h) * 8) & 0xFF);
        }
        return this;
    }
    public ByteArrayTarget add(byte b) {
        if(dataStream != null) {
            dataStream.write(b);
        } else if(data != null) {
            data[offset++] = b;
        }
        return this;
    }

    public ByteArrayTarget add(long[] array) {
        return add(array, 0, array.length);
    }
    public ByteArrayTarget add(long[] array, int arrayOffset, int length) {
        for(int i=0; i < length; i++) add(array[arrayOffset + i]);
        return this;
    }
    public ByteArrayTarget add(int[] array) { return add(array, 0, array.length); }
    public ByteArrayTarget add(int[] array, int arrayOffset, int length) {
        for(int i=0; i < length; i++) add(array[arrayOffset + i]);
        return this;
    }
    public ByteArrayTarget add(short[] array) { return add(array, 0, array.length); }
    public ByteArrayTarget add(short[] array, int arrayOffset, int length) {
        for(int i=0; i < length; i++) add(array[arrayOffset + i]);
        return this;
    }
    public ByteArrayTarget add(byte[] array) { return add(array, 0, array.length); }
    public ByteArrayTarget add(byte[] array, int arrayOffset, int length) {
        if(data != null) {
            System.arraycopy(array, arrayOffset, data, offset, length);
            offset += length;
        }
        if(dataStream != null) {
            dataStream.write(array, arrayOffset, length);
        }
        return this;
    }

    public ByteArrayTarget add(String s) {
        final byte[] sData = s.getBytes();
        add((short)sData.length);
        add(sData);
        return this;
    }

    public byte[] toByteArray() { return dataStream == null ? data : dataStream.toByteArray(); }
    public ByteArrayTarget writeTo(File target) {
        if(!target.getParentFile().exists()) target.getParentFile().mkdirs();
        Utils.writeByteArrayToFile(target, dataStream != null ? dataStream.toByteArray() : data == null ? new byte[0] : data);
        return this;
    }
}
