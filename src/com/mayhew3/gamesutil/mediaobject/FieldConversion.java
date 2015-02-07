package com.mayhew3.gamesutil.mediaobject;

public abstract class FieldConversion<T> {
  abstract T parseFromString(String value) throws NumberFormatException;
}
