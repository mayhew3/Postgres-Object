package com.mayhew3.gamesutil.mediaobjectpostgres;

public abstract class FieldConversion<T> {
  abstract T parseFromString(String value) throws NumberFormatException;
}
