// Copyright 2012 Google Inc. All Rights Reserved.

package com.google.android.gms.common.internal.safeparcel;

/**
 * For docs, see SafeParcelable definition in OneUp/client/common/...
 *
 * <p>This is here for the SafeParcelProcessor to link against and is intentionally not implementing
 * a Parcelable, so that it is not necessary to link in the Android framework to compile this.
 */
public interface SafeParcelable {
    public static final String NULL = "SAFE_PARCELABLE_NULL_STRING";

    public @interface Class {
        String creator();

        boolean validate() default false;

        boolean doNotParcelTypeDefaultValues() default false;
    }

    public @interface Field {
        int id();

        String getter() default NULL;

        String type() default NULL;

        String defaultValue() default NULL;

        String defaultValueUnchecked() default NULL;
    }

    public @interface VersionField {
        int id();

        String getter() default NULL;

        String type() default NULL;
    }

    public @interface Indicator {
        String getter() default NULL;
    }

    public @interface Constructor {}

    public @interface Param {
        int id();
    }

    public @interface RemovedParam {
        int id();

        String defaultValue() default NULL;

        String defaultValueUnchecked() default NULL;
    }

    public @interface Reserved {
        int[] value();
    }
}
