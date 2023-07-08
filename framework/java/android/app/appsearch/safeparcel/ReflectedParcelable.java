package com.google.android.gms.common.internal;

import android.os.Parcelable;

/**
 * Interface for Parcelables that have the class name reflectively read as part of serialization.
 * This happens when when put into an Intent or Bundle, or in some Parcel write methods.
 *
 * <p>This interface is needed because the errorprone checker has some limitations on detecting
 * annotations (like {@code @KeepName}), where detecting inheritance is easier.
 *
 * @see go/gmscore-perf/reflectedparcelable
 * @hide
 */
public interface ReflectedParcelable extends Parcelable {}
