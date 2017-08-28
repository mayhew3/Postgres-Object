package com.mayhew3.mediamogul.dataobject;

public abstract class FieldConversion<T> {
  abstract T parseFromString(String value) throws NumberFormatException;
}
