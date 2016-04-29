package com.mayhew3.gamesutil.db;

import java.net.URISyntaxException;
import java.sql.SQLException;

public abstract class ConnectionFactory {

  abstract SQLConnection createConnection(String indentifier) throws URISyntaxException, SQLException;
}
