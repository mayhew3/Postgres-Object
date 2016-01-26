package com.mayhew3.gamesutil.mediaobject;

import com.mayhew3.gamesutil.games.PostgresConnection;
import org.junit.Before;
import org.junit.Test;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.fest.assertions.Assertions.*;
import static org.mockito.Mockito.*;

public class MediaObjectPostgreSQLTest {

  private MediaObjectMock mediaObject;

  @Before
  public void setUp() {
    mediaObject = new MediaObjectMock();
  }

  @Test
  public void testInitializeForInsert() {
    assertThat(mediaObject.isInitialized())
        .as("SANITY: Expect initialized to be false before initialize method is called.")
        .isFalse();
    assertThat(mediaObject.isForInsert())
        .isFalse();
    assertThat(mediaObject.isForUpdate())
        .isFalse();

    mediaObject.initializeForInsert();

    assertThat(mediaObject.isInitialized())
        .as("Expect object to be initialized.")
        .isTrue();
    assertThat(mediaObject.isForInsert())
        .as("Initialized for insert.")
        .isTrue();
    assertThat(mediaObject.isForUpdate())
        .isFalse();
  }

  @Test
  public void testChangeToUpdateObject() {
    mediaObject.initializeForInsert();

    assertThat(mediaObject.isInitialized())
        .as("SANITY: Expect object to be initialized.")
        .isTrue();
    assertThat(mediaObject.isForInsert())
        .as("SANITY: Initialized for insert.")
        .isTrue();
    assertThat(mediaObject.isForUpdate())
        .isFalse();

    mediaObject.changeToUpdateObject();

    assertThat(mediaObject.isInitialized())
        .as("Expect object to still be initialized after change to 'update'.")
        .isTrue();
    assertThat(mediaObject.isForInsert())
        .as("Should no longer be for insert after change.")
        .isFalse();
    assertThat(mediaObject.isForUpdate())
        .as("Should be an update object after change.")
        .isTrue();
  }

  @Test(expected = IllegalStateException.class)
  public void testChangeToUpdateObjectThrowsExceptionIfNotInitialized() {
    mediaObject.changeToUpdateObject();
  }

  @Test
  public void testInitializeFromDBObject() throws SQLException {
    Integer INITIAL_ID = 2;

    ResultSet resultSet = mock(ResultSet.class);

    when(resultSet.wasNull()).thenReturn(false);
    when(resultSet.getInt("id")).thenReturn(INITIAL_ID);

    mediaObject.initializeFromDBObject(resultSet);

    assertThat(mediaObject.isInitialized())
        .isTrue();
    assertThat(mediaObject.isForUpdate())
        .isTrue();
    assertThat(mediaObject.isForInsert())
        .isFalse();
    assertThat(mediaObject.id.getValue())
        .isEqualTo(INITIAL_ID);
  }


  protected class MediaObjectMock extends MediaObjectPostgreSQL {
    public FieldValueString title = registerStringField("title");

    @Override
    protected String getTableName() {
      return "test";
    }
  }
}