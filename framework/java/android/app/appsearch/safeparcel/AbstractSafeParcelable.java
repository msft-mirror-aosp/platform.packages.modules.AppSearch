// Copyright 2012 Google Inc. All Rights Reserved.

package com.google.android.gms.common.internal.safeparcel;

/**
 * Implements {@link SafeParcelable} and implements some default methods defined by {@link
 * android.os.Parcelable}.
 *
 * @hide
 */
public abstract class AbstractSafeParcelable implements SafeParcelable {

  /** @hide */
  public final int describeContents() {
    return 0;
  }
}
