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
import java.util.ArrayList;
import java.util.List;

/**
 * Functions to read in a safe parcel.
 *
 * @hide
 */
public class SafeParcelReader {

    public static class ParseException extends RuntimeException {
        public ParseException(String message, Parcel p) {
            super(message + " Parcel: pos=" + p.dataPosition() + " size=" + p.dataSize());
        }
    }

    private SafeParcelReader() {}

    public static int readHeader(Parcel p) {
        return p.readInt();
    }

    public static int getFieldId(int header) {
        return header & 0x0000ffff;
    }

    public static int readSize(Parcel p, int header) {
        if ((header & 0xffff0000) != 0xffff0000) {
            return (header >> 16) & 0x0000ffff;
        } else {
            return p.readInt();
        }
    }

    public static void skipUnknownField(Parcel p, int header) {
        int size = readSize(p, header);
        p.setDataPosition(p.dataPosition() + size);
    }

    private static void readAndEnforceSize(Parcel p, int header, int required) {
        final int size = readSize(p, header);
        if (size != required) {
            throw new ParseException(
                    "Expected size "
                            + required
                            + " got "
                            + size
                            + " (0x"
                            + Integer.toHexString(size)
                            + ")",
                    p);
        }
    }

    private static void enforceSize(Parcel p, int header, int size, int required) {
        if (size != required) {
            throw new ParseException(
                    "Expected size "
                            + required
                            + " got "
                            + size
                            + " (0x"
                            + Integer.toHexString(size)
                            + ")",
                    p);
        }
    }

    /** Returns the end position of the object in the parcel. */
    public static int validateObjectHeader(Parcel p) {
        final int header = readHeader(p);
        final int size = readSize(p, header);
        final int start = p.dataPosition();
        if (getFieldId(header) != SafeParcelWriter.OBJECT_HEADER) {
            throw new ParseException(
                    "Expected object header. Got 0x" + Integer.toHexString(header), p);
        }
        final int end = start + size;
        if (end < start || end > p.dataSize()) {
            throw new ParseException("Size read is invalid start=" + start + " end=" + end, p);
        }
        return end;
    }

    public static boolean readBoolean(Parcel p, int header) {
        readAndEnforceSize(p, header, 4);
        return p.readInt() != 0;
    }

    public static Boolean readBooleanObject(Parcel p, int header) {
        final int size = readSize(p, header);
        if (size == 0) {
            return null;
        } else {
            enforceSize(p, header, size, 4);
            return p.readInt() != 0;
        }
    }

    public static byte readByte(Parcel p, int header) {
        readAndEnforceSize(p, header, 4);
        return (byte) p.readInt();
    }

    public static char readChar(Parcel p, int header) {
        readAndEnforceSize(p, header, 4);
        return (char) p.readInt();
    }

    public static short readShort(Parcel p, int header) {
        readAndEnforceSize(p, header, 4);
        return (short) p.readInt();
    }

    public static int readInt(Parcel p, int header) {
        readAndEnforceSize(p, header, 4);
        return p.readInt();
    }

    public static Integer readIntegerObject(Parcel p, int header) {
        final int size = readSize(p, header);
        if (size == 0) {
            return null;
        } else {
            enforceSize(p, header, size, 4);
            return p.readInt();
        }
    }

    public static long readLong(Parcel p, int header) {
        readAndEnforceSize(p, header, 8);
        return p.readLong();
    }

    public static Long readLongObject(Parcel p, int header) {
        final int size = readSize(p, header);
        if (size == 0) {
            return null;
        } else {
            enforceSize(p, header, size, 8);
            return p.readLong();
        }
    }

    public static BigInteger createBigInteger(Parcel p, int header) {
        final int size = readSize(p, header);
        final int pos = p.dataPosition();
        if (size == 0) {
            return null;
        }
        final byte[] val = p.createByteArray();
        p.setDataPosition(pos + size);
        return new BigInteger(val);
    }

    public static float readFloat(Parcel p, int header) {
        readAndEnforceSize(p, header, 4);
        return p.readFloat();
    }

    public static Float readFloatObject(Parcel p, int header) {
        final int size = readSize(p, header);
        if (size == 0) {
            return null;
        } else {
            enforceSize(p, header, size, 4);
            return p.readFloat();
        }
    }

    public static double readDouble(Parcel p, int header) {
        readAndEnforceSize(p, header, 8);
        return p.readDouble();
    }

    public static Double readDoubleObject(Parcel p, int header) {
        final int size = readSize(p, header);
        if (size == 0) {
            return null;
        } else {
            enforceSize(p, header, size, 8);
            return p.readDouble();
        }
    }

    public static BigDecimal createBigDecimal(Parcel p, int header) {
        final int size = readSize(p, header);
        final int pos = p.dataPosition();
        if (size == 0) {
            return null;
        }
        final byte[] unscaledValue = p.createByteArray();
        final int scale = p.readInt();
        p.setDataPosition(pos + size);
        return new BigDecimal(new BigInteger(unscaledValue), scale);
    }

    public static String createString(Parcel p, int header) {
        final int size = readSize(p, header);
        final int pos = p.dataPosition();
        if (size == 0) {
            return null;
        }
        final String result = p.readString();
        p.setDataPosition(pos + size);
        return result;
    }

    public static IBinder readIBinder(Parcel p, int header) {
        final int size = readSize(p, header);
        final int pos = p.dataPosition();
        if (size == 0) {
            return null;
        }
        final IBinder result = p.readStrongBinder();
        p.setDataPosition(pos + size);
        return result;
    }

    public static PendingIntent readPendingIntent(Parcel p, int header) {
        final int size = readSize(p, header);
        final int pos = p.dataPosition();
        if (size == 0) {
            return null;
        }
        final PendingIntent result = PendingIntent.readPendingIntentOrNullFromParcel(p);
        p.setDataPosition(pos + size);
        return result;
    }

    public static <T extends Parcelable> T createParcelable(
            Parcel p, int header, Parcelable.Creator<T> creator) {
        final int size = readSize(p, header);
        final int pos = p.dataPosition();
        if (size == 0) {
            return null;
        }
        final T result = creator.createFromParcel(p);
        p.setDataPosition(pos + size);
        return result;
    }

    public static Bundle createBundle(Parcel p, int header) {
        final int size = readSize(p, header);
        final int pos = p.dataPosition();
        if (size == 0) {
            return null;
        }
        final Bundle result = p.readBundle();
        p.setDataPosition(pos + size);
        return result;
    }

    public static byte[] createByteArray(Parcel p, int header) {
        final int size = readSize(p, header);
        final int pos = p.dataPosition();
        if (size == 0) {
            return null;
        }
        final byte[] result = p.createByteArray();
        p.setDataPosition(pos + size);
        return result;
    }

    public static byte[][] createByteArrayArray(Parcel p, int header) {
        final int size = readSize(p, header);
        final int pos = p.dataPosition();
        if (size == 0) {
            return null;
        }
        final int length = p.readInt();
        final byte[][] result = new byte[length][];
        for (int i = 0; i < length; i++) {
            result[i] = p.createByteArray();
        }
        p.setDataPosition(pos + size);
        return result;
    }

    public static boolean[] createBooleanArray(Parcel p, int header) {
        final int size = readSize(p, header);
        final int pos = p.dataPosition();
        if (size == 0) {
            return null;
        }
        final boolean[] result = p.createBooleanArray();
        p.setDataPosition(pos + size);
        return result;
    }

    public static char[] createCharArray(Parcel p, int header) {
        final int size = readSize(p, header);
        final int pos = p.dataPosition();
        if (size == 0) {
            return null;
        }
        final char[] result = p.createCharArray();
        p.setDataPosition(pos + size);
        return result;
    }

    public static int[] createIntArray(Parcel p, int header) {
        final int size = readSize(p, header);
        final int pos = p.dataPosition();
        if (size == 0) {
            return null;
        }
        final int[] result = p.createIntArray();
        p.setDataPosition(pos + size);
        return result;
    }

    public static long[] createLongArray(Parcel p, int header) {
        final int size = readSize(p, header);
        final int pos = p.dataPosition();
        if (size == 0) {
            return null;
        }
        final long[] result = p.createLongArray();
        p.setDataPosition(pos + size);
        return result;
    }

    public static BigInteger[] createBigIntegerArray(Parcel p, int header) {
        final int size = readSize(p, header);
        final int pos = p.dataPosition();
        if (size == 0) {
            return null;
        }
        final int length = p.readInt();
        final BigInteger[] result = new BigInteger[length];
        for (int i = 0; i < length; i++) {
            result[i] = new BigInteger(p.createByteArray());
        }
        p.setDataPosition(pos + size);
        return result;
    }

    public static float[] createFloatArray(Parcel p, int header) {
        final int size = readSize(p, header);
        final int pos = p.dataPosition();
        if (size == 0) {
            return null;
        }
        final float[] result = p.createFloatArray();
        p.setDataPosition(pos + size);
        return result;
    }

    public static double[] createDoubleArray(Parcel p, int header) {
        final int size = readSize(p, header);
        final int pos = p.dataPosition();
        if (size == 0) {
            return null;
        }
        final double[] result = p.createDoubleArray();
        p.setDataPosition(pos + size);
        return result;
    }

    public static BigDecimal[] createBigDecimalArray(Parcel p, int header) {
        final int size = readSize(p, header);
        final int pos = p.dataPosition();
        if (size == 0) {
            return null;
        }
        final int length = p.readInt();
        final BigDecimal[] result = new BigDecimal[length];
        for (int i = 0; i < length; i++) {
            byte[] unscaledValue = p.createByteArray();
            int scale = p.readInt();
            result[i] = new BigDecimal(new BigInteger(unscaledValue), scale);
        }
        p.setDataPosition(pos + size);
        return result;
    }

    public static String[] createStringArray(Parcel p, int header) {
        final int size = readSize(p, header);
        final int pos = p.dataPosition();
        if (size == 0) {
            return null;
        }
        final String[] result = p.createStringArray();
        p.setDataPosition(pos + size);
        return result;
    }

    public static IBinder[] createIBinderArray(Parcel p, int header) {
        final int size = readSize(p, header);
        final int pos = p.dataPosition();
        if (size == 0) {
            return null;
        }
        final IBinder[] result = p.createBinderArray();
        p.setDataPosition(pos + size);
        return result;
    }

    public static ArrayList<Boolean> createBooleanList(Parcel p, int header) {
        final int size = readSize(p, header);
        final int pos = p.dataPosition();
        if (size == 0) {
            return null;
        }
        final ArrayList<Boolean> result = new ArrayList<Boolean>();
        final int count = p.readInt();
        for (int i = 0; i < count; i++) {
            result.add(p.readInt() != 0 ? true : false);
        }
        p.setDataPosition(pos + size);
        return result;
    }

    public static ArrayList<Integer> createIntegerList(Parcel p, int header) {
        final int size = readSize(p, header);
        final int pos = p.dataPosition();
        if (size == 0) {
            return null;
        }
        final ArrayList<Integer> result = new ArrayList<Integer>();
        final int count = p.readInt();
        for (int i = 0; i < count; i++) {
            result.add(p.readInt());
        }
        p.setDataPosition(pos + size);
        return result;
    }

    public static SparseBooleanArray createSparseBooleanArray(Parcel p, int header) {
        final int size = readSize(p, header);
        final int pos = p.dataPosition();
        if (size == 0) {
            return null;
        }
        SparseBooleanArray result = p.readSparseBooleanArray();
        p.setDataPosition(pos + size);
        return result;
    }

    public static SparseIntArray createSparseIntArray(Parcel p, int header) {
        final int size = readSize(p, header);
        final int pos = p.dataPosition();
        if (size == 0) {
            return null;
        }
        final SparseIntArray result = new SparseIntArray();
        final int count = p.readInt();
        for (int i = 0; i < count; i++) {
            int key = p.readInt();
            int value = p.readInt();
            result.append(key, value);
        }
        p.setDataPosition(pos + size);
        return result;
    }

    public static SparseArray<Float> createFloatSparseArray(Parcel p, int header) {
        final int size = readSize(p, header);
        final int pos = p.dataPosition();
        if (size == 0) {
            return null;
        }
        final SparseArray<Float> result = new SparseArray<Float>();
        final int count = p.readInt();
        for (int i = 0; i < count; i++) {
            int key = p.readInt();
            float value = p.readFloat();
            result.append(key, value);
        }
        p.setDataPosition(pos + size);
        return result;
    }

    public static SparseArray<Double> createDoubleSparseArray(Parcel p, int header) {
        final int size = readSize(p, header);
        final int pos = p.dataPosition();
        if (size == 0) {
            return null;
        }
        final SparseArray<Double> result = new SparseArray<Double>();
        final int count = p.readInt();
        for (int i = 0; i < count; i++) {
            int key = p.readInt();
            double value = p.readDouble();
            result.append(key, value);
        }
        p.setDataPosition(pos + size);
        return result;
    }

    public static SparseLongArray createSparseLongArray(Parcel p, int header) {
        final int size = readSize(p, header);
        final int pos = p.dataPosition();
        if (size == 0) {
            return null;
        }
        final SparseLongArray result = new SparseLongArray();
        final int count = p.readInt();
        for (int i = 0; i < count; i++) {
            int key = p.readInt();
            long value = p.readLong();
            result.append(key, value);
        }
        p.setDataPosition(pos + size);
        return result;
    }

    public static SparseArray<String> createStringSparseArray(Parcel p, int header) {
        final int size = readSize(p, header);
        final int pos = p.dataPosition();
        if (size == 0) {
            return null;
        }
        final SparseArray<String> result = new SparseArray<String>();
        final int count = p.readInt();
        for (int i = 0; i < count; i++) {
            int key = p.readInt();
            String value = p.readString();
            result.append(key, value);
        }
        p.setDataPosition(pos + size);
        return result;
    }

    public static SparseArray<Parcel> createParcelSparseArray(Parcel p, int header) {
        final int size = readSize(p, header);
        final int pos = p.dataPosition();
        if (size == 0) {
            return null;
        }
        final int count = p.readInt();
        final SparseArray<Parcel> result = new SparseArray<Parcel>();
        for (int i = 0; i < count; i++) {
            int key = p.readInt();
            // read in the flag of whether this element is null
            int parcelSize = p.readInt();
            if (parcelSize != 0) {
                // non-null
                int currentDataPosition = p.dataPosition();
                Parcel item = Parcel.obtain();
                item.appendFrom(p, currentDataPosition, parcelSize);
                result.append(key, item);

                // move p's data position
                p.setDataPosition(currentDataPosition + parcelSize);
            } else {
                // is null
                result.append(key, null);
            }
        }
        p.setDataPosition(pos + size);
        return result;
    }

    public static <T> SparseArray<T> createTypedSparseArray(
            Parcel p, int header, Parcelable.Creator<T> c) {
        final int size = readSize(p, header);
        final int pos = p.dataPosition();
        if (size == 0) {
            return null;
        }
        final int count = p.readInt();
        final SparseArray<T> result = new SparseArray<>();
        for (int i = 0; i < count; i++) {
            int key = p.readInt();
            T value;
            if (p.readInt() != 0) {
                value = c.createFromParcel(p);
            } else {
                value = null;
            }
            result.append(key, value);
        }
        p.setDataPosition(pos + size);
        return result;
    }

    public static SparseArray<IBinder> createIBinderSparseArray(Parcel p, int header) {
        final int size = readSize(p, header);
        final int pos = p.dataPosition();
        if (size == 0) {
            return null;
        }
        final int count = p.readInt();
        final SparseArray<IBinder> result = new SparseArray<>(count);
        for (int i = 0; i < count; i++) {
            int key = p.readInt();
            IBinder value = p.readStrongBinder();
            result.append(key, value);
        }
        p.setDataPosition(pos + size);
        return result;
    }

    public static SparseArray<byte[]> createByteArraySparseArray(Parcel p, int header) {
        final int size = readSize(p, header);

        final int pos = p.dataPosition();
        if (size == 0) {
            return null;
        }
        final int count = p.readInt();
        final SparseArray<byte[]> result = new SparseArray<byte[]>(count);
        for (int i = 0; i < count; i++) {
            int key = p.readInt();
            byte[] value = p.createByteArray();
            result.append(key, value);
        }
        p.setDataPosition(pos + size);
        return result;
    }

    public static ArrayList<Long> createLongList(Parcel p, int header) {
        final int size = readSize(p, header);
        final int pos = p.dataPosition();
        if (size == 0) {
            return null;
        }
        final ArrayList<Long> result = new ArrayList<Long>();
        final int count = p.readInt();
        for (int i = 0; i < count; i++) {
            result.add(p.readLong());
        }
        p.setDataPosition(pos + size);
        return result;
    }

    public static ArrayList<Float> createFloatList(Parcel p, int header) {
        final int size = readSize(p, header);
        final int pos = p.dataPosition();
        if (size == 0) {
            return null;
        }
        final ArrayList<Float> result = new ArrayList<Float>();
        final int count = p.readInt();
        for (int i = 0; i < count; i++) {
            result.add(p.readFloat());
        }
        p.setDataPosition(pos + size);
        return result;
    }

    public static ArrayList<Double> createDoubleList(Parcel p, int header) {
        final int size = readSize(p, header);
        final int pos = p.dataPosition();
        if (size == 0) {
            return null;
        }
        final ArrayList<Double> result = new ArrayList<Double>();
        final int count = p.readInt();
        for (int i = 0; i < count; i++) {
            result.add(p.readDouble());
        }
        p.setDataPosition(pos + size);
        return result;
    }

    public static ArrayList<String> createStringList(Parcel p, int header) {
        final int size = readSize(p, header);
        final int pos = p.dataPosition();
        if (size == 0) {
            return null;
        }
        final ArrayList<String> result = p.createStringArrayList();
        p.setDataPosition(pos + size);
        return result;
    }

    public static ArrayList<IBinder> createIBinderList(Parcel p, int header) {
        final int size = readSize(p, header);
        final int pos = p.dataPosition();
        if (size == 0) {
            return null;
        }
        final ArrayList<IBinder> result = p.createBinderArrayList();
        p.setDataPosition(pos + size);
        return result;
    }

    public static <T> T[] createTypedArray(Parcel p, int header, Parcelable.Creator<T> c) {
        final int size = readSize(p, header);
        final int pos = p.dataPosition();
        if (size == 0) {
            return null;
        }
        final T[] result = p.createTypedArray(c);
        p.setDataPosition(pos + size);
        return result;
    }

    public static <T> ArrayList<T> createTypedList(Parcel p, int header, Parcelable.Creator<T> c) {
        final int size = readSize(p, header);
        final int pos = p.dataPosition();
        if (size == 0) {
            return null;
        }
        final ArrayList<T> result = p.createTypedArrayList(c);
        p.setDataPosition(pos + size);
        return result;
    }

    public static Parcel createParcel(Parcel p, int header) {
        final int size = readSize(p, header);
        final int pos = p.dataPosition();
        if (size == 0) {
            return null;
        }
        final Parcel result = Parcel.obtain();
        result.appendFrom(p, pos, size);
        p.setDataPosition(pos + size);
        return result;
    }

    public static Parcel[] createParcelArray(Parcel p, int header) {
        final int size = readSize(p, header);
        final int pos = p.dataPosition();
        if (size == 0) {
            return null;
        }
        final int length = p.readInt();
        final Parcel[] result = new Parcel[length];
        for (int i = 0; i < length; i++) {
            int parcelSize = p.readInt();
            if (parcelSize != 0) {
                int currentDataPosition = p.dataPosition();
                Parcel item = Parcel.obtain();
                item.appendFrom(p, currentDataPosition, parcelSize);
                result[i] = item;

                // move p's data position
                p.setDataPosition(currentDataPosition + parcelSize);
            } else {
                result[i] = null;
            }
        }
        p.setDataPosition(pos + size);
        return result;
    }

    public static ArrayList<Parcel> createParcelList(Parcel p, int header) {
        final int size = readSize(p, header);
        final int pos = p.dataPosition();
        if (size == 0) {
            return null;
        }
        final int length = p.readInt();
        final ArrayList<Parcel> result = new ArrayList<Parcel>();
        for (int i = 0; i < length; i++) {
            // read in the flag of whether this element is null
            int parcelSize = p.readInt();
            if (parcelSize != 0) {
                // non-null
                int currentDataPosition = p.dataPosition();
                Parcel item = Parcel.obtain();
                item.appendFrom(p, currentDataPosition, parcelSize);
                result.add(item);

                // move p's data position
                p.setDataPosition(currentDataPosition + parcelSize);
            } else {
                // is null
                result.add(null);
            }
        }
        p.setDataPosition(pos + size);
        return result;
    }

    public static void readList(
            Parcel p, int header, @SuppressWarnings("rawtypes") List list, ClassLoader loader) {
        final int size = readSize(p, header);
        final int pos = p.dataPosition();
        if (size == 0) {
            return;
        }
        p.readList(list, loader);
        p.setDataPosition(pos + size);
    }

    public static void ensureAtEnd(Parcel parcel, int end) {
        if (parcel.dataPosition() != end) {
            throw new ParseException("Overread allowed size end=" + end, parcel);
        }
    }
}
