package com.mayhew3.gamesutil.dataobject;

public abstract class FieldConversion<T> {
  abstract T parseFromString(String value) throws NumberFormatException;
}
