// Copyright 2012 Google Inc. All Rights Reserved.

package com.google.android.gms.common.internal.safeparcel;

import android.app.PendingIntent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.util.SparseLongArray;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

/**
 * Functions to write a safe parcel. A safe parcel consists of a sequence of header/payload bytes.
 *
 * <p>The header is 16 bits of size and 16 bits of field id. If the size in the header is 0xffff,
 * the next 4 bytes are the size field instead.
 *
 * @hide
 */
public class SafeParcelWriter {

    static final int OBJECT_HEADER = 0x00004f45;

    private SafeParcelWriter() {}

    private static void writeHeader(Parcel p, int id, int size) {
        if (size >= 0x0000ffff) {
            p.writeInt(0xffff0000 | id);
            p.writeInt(size);
        } else {
            p.writeInt((size << 16) | id);
        }
    }

    /** Returns the cookie that should be passed to endVariableData. */
    private static int beginVariableData(Parcel p, int id) {
        // Since we don't know the size yet, assume it might be big and always use the
        // size overflow.
        p.writeInt(0xffff0000 | id);
        p.writeInt(0);
        return p.dataPosition();
    }

    /**
     * @param start The result of the paired beginVariableData.
     */
    private static void finishVariableData(Parcel p, int start) {
        int end = p.dataPosition();
        int size = end - start;
        // The size is one int before start.
        p.setDataPosition(start - 4);
        p.writeInt(size);
        p.setDataPosition(end);
    }

    public static int beginObjectHeader(Parcel p) {
        return beginVariableData(p, OBJECT_HEADER);
    }

    public static void finishObjectHeader(Parcel p, int start) {
        finishVariableData(p, start);
    }

    public static void writeBoolean(Parcel p, int id, boolean val) {
        writeHeader(p, id, 4);
        p.writeInt(val ? 1 : 0);
    }

    public static void writeBooleanObject(Parcel p, int id, Boolean val, boolean writeNull) {
        if (val == null) {
            if (writeNull) {
                writeHeader(p, id, 0);
            }
            return;
        }

        writeHeader(p, id, 4);
        p.writeInt(val ? 1 : 0);
    }

    public static void writeByte(Parcel p, int id, byte val) {
        writeHeader(p, id, 4);
        p.writeInt(val);
    }

    public static void writeChar(Parcel p, int id, char val) {
        writeHeader(p, id, 4);
        p.writeInt(val);
    }

    public static void writeShort(Parcel p, int id, short val) {
        writeHeader(p, id, 4);
        p.writeInt(val);
    }

    public static void writeInt(Parcel p, int id, int val) {
        writeHeader(p, id, 4);
        p.writeInt(val);
    }

    public static void writeIntegerObject(Parcel p, int id, Integer val, boolean writeNull) {
        if (val == null) {
            if (writeNull) {
                writeHeader(p, id, 0);
            }
            return;
        }
        writeHeader(p, id, 4);
        p.writeInt(val);
    }

    public static void writeLong(Parcel p, int id, long val) {
        writeHeader(p, id, 8);
        p.writeLong(val);
    }

    public static void writeLongObject(Parcel p, int id, Long val, boolean writeNull) {
        if (val == null) {
            if (writeNull) {
                writeHeader(p, id, 0);
            }
            return;
        }
        writeHeader(p, id, 8);
        p.writeLong(val);
    }

    public static void writeBigInteger(Parcel p, int id, BigInteger val, boolean writeNull) {
        if (val == null) {
            if (writeNull) {
                writeHeader(p, id, 0);
            }
            return;
        }
        int start = beginVariableData(p, id);
        p.writeByteArray(val.toByteArray());
        finishVariableData(p, start);
    }

    public static void writeFloat(Parcel p, int id, float val) {
        writeHeader(p, id, 4);
        p.writeFloat(val);
    }

    public static void writeFloatObject(Parcel p, int id, Float val, boolean writeNull) {
        if (val == null) {
            if (writeNull) {
                writeHeader(p, id, 0);
            }
            return;
        }
        writeHeader(p, id, 4);
        p.writeFloat(val);
    }

    public static void writeDouble(Parcel p, int id, double val) {
        writeHeader(p, id, 8);
        p.writeDouble(val);
    }

    public static void writeDoubleObject(Parcel p, int id, Double val, boolean writeNull) {
        if (val == null) {
            if (writeNull) {
                writeHeader(p, id, 0);
            }
            return;
        }
        writeHeader(p, id, 8);
        p.writeDouble(val);
    }

    public static void writeBigDecimal(Parcel p, int id, BigDecimal val, boolean writeNull) {
        if (val == null) {
            if (writeNull) {
                writeHeader(p, id, 0);
            }
            return;
        }
        int start = beginVariableData(p, id);
        p.writeByteArray(val.unscaledValue().toByteArray());
        p.writeInt(val.scale());
        finishVariableData(p, start);
    }

    public static void writeString(Parcel p, int id, String val, boolean writeNull) {
        if (val == null) {
            if (writeNull) {
                writeHeader(p, id, 0);
            }
            return;
        }
        int start = beginVariableData(p, id);
        p.writeString(val);
        finishVariableData(p, start);
    }

    public static void writeIBinder(Parcel p, int id, IBinder val, boolean writeNull) {
        if (val == null) {
            if (writeNull) {
                writeHeader(p, id, 0);
            }
            return;
        }
        // The size of the flat_binder_object in Parcel.cpp is not actually variable
        // but is not part of the CDD, so treat it as variable.  It almost certainly
        // won't change between processes on a given device.
        int start = beginVariableData(p, id);
        p.writeStrongBinder(val);
        finishVariableData(p, start);
    }

    public static void writeParcelable(
            Parcel p, int id, Parcelable val, int parcelableFlags, boolean writeNull) {
        if (val == null) {
            if (writeNull) {
                writeHeader(p, id, 0);
            }
            return;
        }
        int start = beginVariableData(p, id);
        val.writeToParcel(p, parcelableFlags);
        finishVariableData(p, start);
    }

    public static void writeBundle(Parcel p, int id, Bundle val, boolean writeNull) {
        if (val == null) {
            if (writeNull) {
                writeHeader(p, id, 0);
            }
            return;
        }
        int start = beginVariableData(p, id);
        p.writeBundle(val);
        finishVariableData(p, start);
    }

    public static void writeByteArray(Parcel p, int id, byte[] buf, boolean writeNull) {
        if (buf == null) {
            if (writeNull) {
                writeHeader(p, id, 0);
            }
            return;
        }
        int start = beginVariableData(p, id);
        p.writeByteArray(buf);
        finishVariableData(p, start);
    }

    public static void writeByteArrayArray(Parcel p, int id, byte[][] buf, boolean writeNull) {
        if (buf == null) {
            if (writeNull) {
                writeHeader(p, id, 0);
            }
            return;
        }
        int start = beginVariableData(p, id);
        final int length = buf.length;
        p.writeInt(length);
        for (int i = 0; i < length; i++) {
            p.writeByteArray(buf[i]);
        }
        finishVariableData(p, start);
    }

    public static void writeBooleanArray(Parcel p, int id, boolean[] val, boolean writeNull) {
        if (val == null) {
            if (writeNull) {
                writeHeader(p, id, 0);
            }
            return;
        }
        int start = beginVariableData(p, id);
        p.writeBooleanArray(val);
        finishVariableData(p, start);
    }

    public static void writeCharArray(Parcel p, int id, char[] val, boolean writeNull) {
        if (val == null) {
            if (writeNull) {
                writeHeader(p, id, 0);
            }
            return;
        }
        int start = beginVariableData(p, id);
        p.writeCharArray(val);
        finishVariableData(p, start);
    }

    public static void writeIntArray(Parcel p, int id, int[] val, boolean writeNull) {
        if (val == null) {
            if (writeNull) {
                writeHeader(p, id, 0);
            }
            return;
        }
        int start = beginVariableData(p, id);
        p.writeIntArray(val);
        finishVariableData(p, start);
    }

    public static void writeLongArray(Parcel p, int id, long[] val, boolean writeNull) {
        if (val == null) {
            if (writeNull) {
                writeHeader(p, id, 0);
            }
            return;
        }
        int start = beginVariableData(p, id);
        p.writeLongArray(val);
        finishVariableData(p, start);
    }

    public static void writeBigIntegerArray(Parcel p, int id, BigInteger[] val, boolean writeNull) {
        if (val == null) {
            if (writeNull) {
                writeHeader(p, id, 0);
            }
            return;
        }
        int start = beginVariableData(p, id);
        final int length = val.length;
        p.writeInt(length);
        for (int i = 0; i < length; i++) {
            p.writeByteArray(val[i].toByteArray());
        }
        finishVariableData(p, start);
    }

    public static void writeFloatArray(Parcel p, int id, float[] val, boolean writeNull) {
        if (val == null) {
            if (writeNull) {
                writeHeader(p, id, 0);
            }
            return;
        }
        int start = beginVariableData(p, id);
        p.writeFloatArray(val);
        finishVariableData(p, start);
    }

    public static void writeDoubleArray(Parcel p, int id, double[] val, boolean writeNull) {
        if (val == null) {
            if (writeNull) {
                writeHeader(p, id, 0);
            }
            return;
        }
        int start = beginVariableData(p, id);
        p.writeDoubleArray(val);
        finishVariableData(p, start);
    }

    public static void writeBigDecimalArray(Parcel p, int id, BigDecimal[] val, boolean writeNull) {
        if (val == null) {
            if (writeNull) {
                writeHeader(p, id, 0);
            }
            return;
        }
        int start = beginVariableData(p, id);
        final int length = val.length;
        p.writeInt(length);
        for (int i = 0; i < length; i++) {
            p.writeByteArray(val[i].unscaledValue().toByteArray());
            p.writeInt(val[i].scale());
        }
        finishVariableData(p, start);
    }

    public static void writeStringArray(Parcel p, int id, String[] val, boolean writeNull) {
        if (val == null) {
            if (writeNull) {
                writeHeader(p, id, 0);
            }
            return;
        }
        int start = beginVariableData(p, id);
        p.writeStringArray(val);
        finishVariableData(p, start);
    }

    public static void writeIBinderArray(Parcel p, int id, IBinder[] val, boolean writeNull) {
        if (val == null) {
            if (writeNull) {
                writeHeader(p, id, 0);
            }
            return;
        }
        int start = beginVariableData(p, id);
        p.writeBinderArray(val);
        finishVariableData(p, start);
    }

    public static void writeBooleanList(Parcel p, int id, List<Boolean> val, boolean writeNull) {
        if (val == null) {
            if (writeNull) {
                writeHeader(p, id, 0);
            }
            return;
        }
        int start = beginVariableData(p, id);
        final int size = val.size();
        p.writeInt(size);
        for (int i = 0; i < size; i++) {
            p.writeInt(val.get(i) ? 1 : 0);
        }
        finishVariableData(p, start);
    }

    public static void writeIntegerList(Parcel p, int id, List<Integer> val, boolean writeNull) {
        if (val == null) {
            if (writeNull) {
                writeHeader(p, id, 0);
            }
            return;
        }
        int start = beginVariableData(p, id);
        final int size = val.size();
        p.writeInt(size);
        for (int i = 0; i < size; i++) {
            p.writeInt(val.get(i));
        }
        finishVariableData(p, start);
    }

    public static void writeLongList(Parcel p, int id, List<Long> val, boolean writeNull) {
        if (val == null) {
            if (writeNull) {
                writeHeader(p, id, 0);
            }
            return;
        }
        int start = beginVariableData(p, id);
        final int size = val.size();
        p.writeInt(size);
        for (int i = 0; i < size; i++) {
            p.writeLong(val.get(i));
        }
        finishVariableData(p, start);
    }

    public static void writeFloatList(Parcel p, int id, List<Float> val, boolean writeNull) {
        if (val == null) {
            if (writeNull) {
                writeHeader(p, id, 0);
            }
            return;
        }
        int start = beginVariableData(p, id);
        final int size = val.size();
        p.writeInt(size);
        for (int i = 0; i < size; i++) {
            p.writeFloat(val.get(i));
        }
        finishVariableData(p, start);
    }

    public static void writeDoubleList(Parcel p, int id, List<Double> val, boolean writeNull) {
        if (val == null) {
            if (writeNull) {
                writeHeader(p, id, 0);
            }
            return;
        }
        int start = beginVariableData(p, id);
        final int size = val.size();
        p.writeInt(size);
        for (int i = 0; i < size; i++) {
            p.writeDouble(val.get(i));
        }
        finishVariableData(p, start);
    }

    public static void writeStringList(Parcel p, int id, List<String> val, boolean writeNull) {
        if (val == null) {
            if (writeNull) {
                writeHeader(p, id, 0);
            }
            return;
        }
        int start = beginVariableData(p, id);
        p.writeStringList(val);
        finishVariableData(p, start);
    }

    public static void writeIBinderList(Parcel p, int id, List<IBinder> val, boolean writeNull) {
        if (val == null) {
            if (writeNull) {
                writeHeader(p, id, 0);
            }
            return;
        }
        int start = beginVariableData(p, id);
        p.writeBinderList(val);
        finishVariableData(p, start);
    }

    public static <T extends Parcelable> void writeTypedArray(
            Parcel p, int id, T[] val, int parcelableFlags, boolean writeNull) {
        if (val == null) {
            if (writeNull) {
                writeHeader(p, id, 0);
            }
            return;
        }
        int start = beginVariableData(p, id);
        // We need to customize the built-in Parcel.writeTypedArray() because we need to write
        // the sizes for each individual SafeParcelable objects since they can vary in size due
        // to supporting missing fields.
        final int length = val.length;
        p.writeInt(length);
        for (int i = 0; i < length; i++) {
            T item = val[i];
            if (item == null) {
                p.writeInt(0);
            } else {
                writeTypedItemWithSize(p, item, parcelableFlags);
            }
        }
        finishVariableData(p, start);
    }

    public static <T extends Parcelable> void writeTypedList(
            Parcel p, int id, List<T> val, boolean writeNull) {
        if (val == null) {
            if (writeNull) {
                writeHeader(p, id, 0);
            }
            return;
        }
        int start = beginVariableData(p, id);
        // We need to customize the built-in Parcel.writeTypedList() because we need to write
        // the sizes for each individual SafeParcelable objects since they can vary in size due
        // supporting missing fields.
        final int length = val.size();
        p.writeInt(length);
        for (int i = 0; i < length; i++) {
            T item = val.get(i);
            if (item == null) {
                p.writeInt(0);
            } else {
                writeTypedItemWithSize(p, item, 0);
            }
        }
        finishVariableData(p, start);
    }

    private static <T extends Parcelable> void writeTypedItemWithSize(
            Parcel p, T item, int parcelableFlags) {
        // Just write a 1 as a placeholder since we don't know the exact size of item
        // yet, and save the data position in Parcel p.
        final int itemSizeDataPosition = p.dataPosition();
        p.writeInt(1);
        final int itemStartPosition = p.dataPosition();
        item.writeToParcel(p, parcelableFlags);
        final int currentDataPosition = p.dataPosition();

        // go back and write the length in bytes
        p.setDataPosition(itemSizeDataPosition);
        p.writeInt(currentDataPosition - itemStartPosition);

        // set the parcel data position to where it was before
        p.setDataPosition(currentDataPosition);
    }

    public static void writeParcel(Parcel p, int id, Parcel val, boolean writeNull) {
        if (val == null) {
            if (writeNull) {
                writeHeader(p, id, 0);
            }
            return;
        }
        int start = beginVariableData(p, id);
        p.appendFrom(val, 0, val.dataSize());
        finishVariableData(p, start);
    }

    /**
     * This is made to be compatible with writeTypedArray. See implementation of
     * Parcel.writeTypedArray(T[] val, parcelableFlags);
     */
    public static void writeParcelArray(Parcel p, int id, Parcel[] val, boolean writeNull) {
        if (val == null) {
            if (writeNull) {
                writeHeader(p, id, 0);
            }
            return;
        }
        int start = beginVariableData(p, id);
        final int length = val.length;
        p.writeInt(length);
        for (int i = 0; i < length; i++) {
            Parcel item = val[i];
            if (item != null) {
                p.writeInt(item.dataSize());
                // custom part
                p.appendFrom(item, 0, item.dataSize());
            } else {
                p.writeInt(0);
            }
        }
        finishVariableData(p, start);
    }

    /**
     * This is made to be compatible with writeTypedList. See implementation of
     * Parce.writeTypedList(List<T> val).
     */
    public static void writeParcelList(Parcel p, int id, List<Parcel> val, boolean writeNull) {
        if (val == null) {
            if (writeNull) {
                writeHeader(p, id, 0);
            }
            return;
        }
        int start = beginVariableData(p, id);
        final int size = val.size();
        p.writeInt(size);
        for (int i = 0; i < size; i++) {
            Parcel item = val.get(i);
            if (item != null) {
                p.writeInt(item.dataSize());
                // custom part
                p.appendFrom(item, 0, item.dataSize());
            } else {
                p.writeInt(0);
            }
        }
        finishVariableData(p, start);
    }

    public static void writePendingIntent(Parcel p, int id, PendingIntent val, boolean writeNull) {
        if (val == null) {
            if (writeNull) {
                writeHeader(p, id, 0);
            }
            return;
        }
        int start = beginVariableData(p, id);
        PendingIntent.writePendingIntentOrNullToParcel(val, p);
        finishVariableData(p, start);
    }

    public static void writeList(
            Parcel p, int id, @SuppressWarnings("rawtypes") List list, boolean writeNull) {
        if (list == null) {
            if (writeNull) {
                writeHeader(p, id, 0);
            }
            return;
        }
        int start = beginVariableData(p, id);
        p.writeList(list);
        finishVariableData(p, start);
    }

    public static void writeSparseBooleanArray(
            Parcel p, int id, SparseBooleanArray val, boolean writeNull) {
        if (val == null) {
            if (writeNull) {
                writeHeader(p, id, 0);
            }
            return;
        }
        int start = beginVariableData(p, id);
        p.writeSparseBooleanArray(val);
        finishVariableData(p, start);
    }

    public static void writeDoubleSparseArray(
            Parcel p, int id, SparseArray<Double> val, boolean writeNull) {
        if (val == null) {
            if (writeNull) {
                writeHeader(p, id, 0);
            }
            return;
        }
        int start = beginVariableData(p, id);
        final int size = val.size();
        p.writeInt(size);
        for (int i = 0; i < size; i++) {
            p.writeInt(val.keyAt(i));
            p.writeDouble(val.valueAt(i));
        }
        finishVariableData(p, start);
    }

    public static void writeFloatSparseArray(
            Parcel p, int id, SparseArray<Float> val, boolean writeNull) {
        if (val == null) {
            if (writeNull) {
                writeHeader(p, id, 0);
            }
            return;
        }
        int start = beginVariableData(p, id);
        final int size = val.size();
        p.writeInt(size);
        for (int i = 0; i < size; i++) {
            p.writeInt(val.keyAt(i));
            p.writeFloat(val.valueAt(i));
        }
        finishVariableData(p, start);
    }

    public static void writeSparseIntArray(
            Parcel p, int id, SparseIntArray val, boolean writeNull) {
        if (val == null) {
            if (writeNull) {
                writeHeader(p, id, 0);
            }
            return;
        }
        int start = beginVariableData(p, id);
        final int size = val.size();
        p.writeInt(size);
        for (int i = 0; i < size; i++) {
            p.writeInt(val.keyAt(i));
            p.writeInt(val.valueAt(i));
        }
        finishVariableData(p, start);
    }

    public static void writeSparseLongArray(
            Parcel p, int id, SparseLongArray val, boolean writeNull) {
        if (val == null) {
            if (writeNull) {
                writeHeader(p, id, 0);
            }
            return;
        }
        int start = beginVariableData(p, id);
        final int size = val.size();
        p.writeInt(size);
        for (int i = 0; i < size; i++) {
            p.writeInt(val.keyAt(i));
            p.writeLong(val.valueAt(i));
        }
        finishVariableData(p, start);
    }

    public static void writeStringSparseArray(
            Parcel p, int id, SparseArray<String> val, boolean writeNull) {
        if (val == null) {
            if (writeNull) {
                writeHeader(p, id, 0);
            }
            return;
        }
        int start = beginVariableData(p, id);
        final int size = val.size();
        p.writeInt(size);
        for (int i = 0; i < size; i++) {
            p.writeInt(val.keyAt(i));
            p.writeString(val.valueAt(i));
        }
        finishVariableData(p, start);
    }

    public static void writeParcelSparseArray(
            Parcel p, int id, SparseArray<Parcel> val, boolean writeNull) {
        if (val == null) {
            if (writeNull) {
                writeHeader(p, id, 0);
            }
            return;
        }
        int start = beginVariableData(p, id);
        final int size = val.size();
        p.writeInt(size);
        for (int i = 0; i < size; i++) {
            p.writeInt(val.keyAt(i));
            Parcel item = val.valueAt(i);
            if (item != null) {
                p.writeInt(item.dataSize());
                // custom part
                p.appendFrom(item, 0, item.dataSize());
            } else {
                p.writeInt(0);
            }
        }
        finishVariableData(p, start);
    }

    public static <T extends Parcelable> void writeTypedSparseArray(
            Parcel p, int id, SparseArray<T> val, boolean writeNull) {
        if (val == null) {
            if (writeNull) {
                writeHeader(p, id, 0);
            }
            return;
        }
        int start = beginVariableData(p, id);
        // We follow the same approach as writeTypedList().
        final int size = val.size();
        p.writeInt(size);
        for (int i = 0; i < size; i++) {
            p.writeInt(val.keyAt(i));
            T item = val.valueAt(i);
            if (item == null) {
                p.writeInt(0);
            } else {
                writeTypedItemWithSize(p, item, 0);
            }
        }
        finishVariableData(p, start);
    }

    public static void writeIBinderSparseArray(
            Parcel p, int id, SparseArray<IBinder> val, boolean writeNull) {
        if (val == null) {
            if (writeNull) {
                writeHeader(p, id, 0);
            }
            return;
        }
        int start = beginVariableData(p, id);
        final int size = val.size();
        p.writeInt(size);
        for (int i = 0; i < size; i++) {
            p.writeInt(val.keyAt(i));
            p.writeStrongBinder(val.valueAt(i));
        }
        finishVariableData(p, start);
    }

    public static void writeByteArraySparseArray(
            Parcel p, int id, SparseArray<byte[]> val, boolean writeNull) {
        if (val == null) {
            if (writeNull) {
                writeHeader(p, id, 0);
            }
            return;
        }
        int start = beginVariableData(p, id);
        final int size = val.size();
        p.writeInt(size);
        for (int i = 0; i < size; i++) {
            p.writeInt(val.keyAt(i));
            p.writeByteArray(val.valueAt(i));
        }
        finishVariableData(p, start);
    }
}
