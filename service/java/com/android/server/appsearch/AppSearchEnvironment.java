package com.android.server.appsearch;

import android.annotation.NonNull;
import android.content.Context;
import android.os.UserHandle;
import java.io.File;

/** An interface which exposes environment specific methods for AppSearch. */
public interface AppSearchEnvironment {

  /** Returns the directory to initialize appsearch based on the environment. */
  public File getAppSearchDir(@NonNull Context context, @NonNull UserHandle userHandle);

  /** Returns the correct context for the user based on the environment. */
  public Context createContextAsUser(@NonNull Context context, @NonNull UserHandle userHandle);
}

