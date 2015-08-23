package com.mayhew3.gamesutil.mediaobjectmongo;

public abstract class FieldConversion<T> {
  abstract T parseFromString(String value) throws NumberFormatException;
}
