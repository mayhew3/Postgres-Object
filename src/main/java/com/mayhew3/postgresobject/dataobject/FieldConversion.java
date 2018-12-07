package com.mayhew3.postgresobject.dataobject;

public abstract class FieldConversion<T> {
  abstract T parseFromString(String value) throws NumberFormatException;
}
