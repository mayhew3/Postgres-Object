package com.mayhew3.gamesutil.mediaobject;

import com.mayhew3.gamesutil.db.PostgresConnection;
import org.junit.Before;
import org.junit.Test;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class EpisodePostgresTest {

  private EpisodePostgres episodePostgres;
  private Integer INITIAL_ID = 5;

  @Before
  public void setUp() {
    episodePostgres = new EpisodePostgres();
    episodePostgres.id.initializeValue(INITIAL_ID);
  }

  @Test
  public void testAddToTiVoEpisodes() {

  }

  @Test
  public void testGetTiVoEpisodes() throws SQLException {
    PostgresConnection postgresConnection = mock(PostgresConnection.class);
    ResultSet resultSet = mock(ResultSet.class);

    when(postgresConnection.prepareAndExecuteStatementFetch(anyString(), eq(INITIAL_ID))).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true).thenReturn(false);

    List<TiVoEpisodePostgres> tiVoEpisodes = episodePostgres.getTiVoEpisodes(postgresConnection);

    assertThat(tiVoEpisodes)
        .hasSize(1);

    TiVoEpisodePostgres tiVoEpisodePostgres = tiVoEpisodes.get(0);

    verify(resultSet).getInt("id");

    for (FieldValue fieldValue : tiVoEpisodePostgres.allFieldValues) {
      verify(resultSet).getObject(fieldValue.getFieldName());
    }
  }
}