package dbserver;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ResourceBundle;
import java.util.Stack;
import java.util.logging.Logger;

/**
 *
 * @author Urko
 */
public class PostgresConnectionPool {

    // Logger para registrar eventos y errores
    private static final Logger LOGGER = Logger.getLogger(PostgresConnectionPool.class.getName());

    // Stack para almacenar las conexiones disponibles en el pool
    private Stack<Connection> connectionPool = new Stack<>();

    // Variables para almacenar los datos de conexión a la base de datos
    private String url;
    private String user;
    private String password;

    // Constructor para inicializar el pool de conexiones
    public PostgresConnectionPool(int poolSize) {
        // Cargar los datos de conexión desde el archivo de propiedades
        loadProperties();

        try {
            // Cargar el driver de PostgreSQL
            Class.forName("org.postgresql.Driver");

            // Crear el número de conexiones especificado por poolSize
            for (int i = 0; i < poolSize; i++) {
                Connection conn = DriverManager.getConnection(url, user, password);
                if (conn != null) {
                    // Si la conexión es válida, se añade al pool
                    LOGGER.info("Conexión " + (i + 1) + " creada y añadida al pool.");
                    connectionPool.push(conn);
                } else {
                    // Registrar un aviso si no se puede crear la conexión
                    LOGGER.warning("No se ha podido crear la conexión " + (i + 1));
                }
            }
        } catch (SQLException e) {
            // Manejo de errores relacionados con la base de datos
            LOGGER.warning("Error de conexión a la base de datos.");
        } catch (ClassNotFoundException e) {
            // Manejo de error si el driver no es encontrado
            LOGGER.warning("Driver no encontrado");
        }
    }

    // Método privado para cargar las propiedades de conexión desde un archivo de propiedades
    private void loadProperties() {
        // Cargar el archivo de propiedades dbConnection desde ResourceBundle
        ResourceBundle bundle = ResourceBundle.getBundle("dbserver.dbConnection");
        // Asignar los valores de las propiedades cargadas a las variables url, user y password
        url = bundle.getString("db.url");
        user = bundle.getString("db.user");
        password = bundle.getString("db.password");
    }

    // Método sincronizado para obtener una conexión del pool
    public synchronized Connection getConnection() throws SQLException {
        // Si no hay conexiones disponibles en el pool, se crea una nueva
        if (connectionPool.isEmpty()) {
            LOGGER.info("No hay conexiones en el pool, creando una nueva...");
            return DriverManager.getConnection(url, user, password);
        } else {
            // Si hay conexiones disponibles, se toma una del pool
            LOGGER.info("Conexiones disponibles: " + connectionPool.size());
            return connectionPool.pop();
        }
    }

    // Método sincronizado para liberar una conexión y devolverla al pool
    public synchronized void releaseConnection(Connection connection) {
        // Añadir la conexión de vuelta al pool
        connectionPool.push(connection);
        LOGGER.info("Conexión liberada de vuelta al pool. Quedan: " + connectionPool.size());
    }
}
