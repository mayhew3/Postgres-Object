package com.mayhew3.postgresobject.dataobject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import static org.mockito.Mockito.*;

public class FieldValueBigDecimalTest {

  private final BigDecimal INITIAL_VALUE = BigDecimal.valueOf(8.37);
  private FieldValueBigDecimal fieldValueBigDecimal;

  @BeforeEach
  public void setUp() {
    fieldValueBigDecimal = new FieldValueBigDecimal("testName", mock(FieldConversionBigDecimal.class), Nullability.NOT_NULL);
    fieldValueBigDecimal.initializeValue(INITIAL_VALUE);
  }

  @Test
  public void testUpdatePreparedStatement() throws SQLException {
    PreparedStatement preparedStatement = mock(PreparedStatement.class);

    int currentIndex = 3;

    fieldValueBigDecimal.updatePreparedStatement(preparedStatement, currentIndex);

    verify(preparedStatement).setBigDecimal(currentIndex, INITIAL_VALUE);
  }

  @Test
  public void testUpdatePreparedStatementNullValue() throws SQLException {
    fieldValueBigDecimal.nullValue();

    PreparedStatement preparedStatement = mock(PreparedStatement.class);

    int currentIndex = 3;

    fieldValueBigDecimal.updatePreparedStatement(preparedStatement, currentIndex);

    verify(preparedStatement).setNull(currentIndex, Types.NUMERIC);
  }

  @Test
  public void testChangeValue() {

  }
}