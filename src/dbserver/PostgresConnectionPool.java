package dbserver;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Stack;
import java.util.logging.Logger;

public class PostgresConnectionPool {

    private static final Logger logger = Logger.getLogger(PostgresConnectionPool.class.getName());
    private Stack<Connection> connectionPool = new Stack<>();
    private String url;
    private String user;
    private String password;

    // Constructor para inicializar el pool de conexiones
    public PostgresConnectionPool(int poolSize) {
        // Cargar los datos de conexión desde el archivo de propiedades
        loadProperties();

        try {
            // Asegurarse de que el controlador de PostgreSQL esté cargado
            Class.forName("org.postgresql.Driver");

            for (int i = 0; i < poolSize; i++) {
                Connection conn = DriverManager.getConnection(url, user, password);
                if (conn != null) {
                    logger.info("Conexión " + (i + 1) + " creada y añadida al pool.");
                    connectionPool.push(conn);
                } else {
                    logger.warning("No se ha podido crear la conexión " + (i + 1));
                }
            }
        } catch (SQLException e) {
            logger.warning("Error de conexión a la base de datos.");
        } catch (ClassNotFoundException e) {
            logger.warning("Driver no encontrado");
        }
    }

    // Método para cargar propiedades de un archivo
    private void loadProperties() {
        Properties properties = new Properties();
        ResourceBundle bundle = ResourceBundle.getBundle("dbserver.dbConnection");
        url = bundle.getString("db.url");
        user = bundle.getString("db.user");
        password = bundle.getString("db.password");
    }

    // Método para obtener una conexión del pool
    public synchronized Connection getConnection() throws SQLException {
        if (connectionPool.isEmpty()) {
            logger.info("No hay conexiones en el pool, creando una nueva...");
            return DriverManager.getConnection(url, user, password);
        } else {
            logger.info("Conexiones disponibles: " + connectionPool.size());
            return connectionPool.pop();
        }
    }

    // Método para liberar una conexión y devolverla al pool
    public synchronized void releaseConnection(Connection connection) {
        connectionPool.push(connection);
        logger.info("Conexión liberada de vuelta al pool. Quedan: " + connectionPool.size());
    }
}
