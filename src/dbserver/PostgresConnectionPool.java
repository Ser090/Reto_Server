package dbserver;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ResourceBundle;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;
import utilidades.Closeable;

/**
 * Clase PostgresConnectionPool que gestiona un pool de conexiones a una base de
 * datos PostgreSQL.
 *
 * <p>
 * Implementa la interfaz Closeable para permitir el cierre de todas las
 * conexiones de forma controlada. Esta clase proporciona métodos para obtener y
 * liberar conexiones del pool, asegurando una gestión eficiente de los recursos
 * de la base de datos.
 *
 * @author Urko
 */
public class PostgresConnectionPool implements Closeable {

    /**
     * Logger para registrar eventos y errores.
     */
    private static final Logger LOGGER = Logger.getLogger(PostgresConnectionPool.class.getName());

    /**
     * Stack que almacena las conexiones disponibles en el pool.
     */
    private Stack<Connection> connectionPool = new Stack<>();

    /**
     * Variables para almacenar los datos de conexión a la base de datos.
     */
    private String url;
    private String user;
    private String password;

    /**
     * Constructor para inicializar el pool de conexiones.
     *
     * <p>
     * Se cargan los datos de conexión desde un archivo de propiedades y se
     * crean las conexiones iniciales especificadas por poolSize.
     *
     * @param poolSize el número de conexiones a crear en el pool.
     */
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
                    LOGGER.log(Level.INFO, "Conexión {0} creada y añadida al pool.", i + 1);
                    connectionPool.push(conn);
                } else {
                    // Registrar un aviso si no se puede crear la conexión
                    LOGGER.log(Level.WARNING, "No se ha podido crear la conexión {0}", i + 1);
                }
            }
        } catch (SQLException event) {
            // Manejo de errores relacionados con la base de datos
            LOGGER.warning("Error de conexión a la base de datos.");
        } catch (ClassNotFoundException event) {
            // Manejo de error si el driver no es encontrado
            LOGGER.warning("Driver no encontrado.");
        }
    }

    /**
     * Método privado para cargar las propiedades de conexión desde un archivo
     * de propiedades.
     *
     * <p>
     * Carga los datos necesarios para establecer una conexión a la base de
     * datos desde el archivo de propiedades dbConnection.
     */
    private void loadProperties() {
        // Cargar el archivo de propiedades dbConnection desde ResourceBundle
        ResourceBundle bundle = ResourceBundle.getBundle("dbserver.dbConnection");
        // Asignar los valores de las propiedades cargadas a las variables url, user y password
        url = bundle.getString("db.url");
        user = bundle.getString("db.user");
        password = bundle.getString("db.password");
    }

    /**
     * Método sincronizado para obtener una conexión del pool.
     *
     * <p>
     * Si no hay conexiones disponibles, se devuelve null.
     *
     * @return una conexión disponible o null si no hay conexiones en el pool.
     * @throws SQLException si ocurre un error al obtener la conexión.
     */
    public synchronized Connection getConnection() throws SQLException {
        // Si no hay conexiones disponibles en el pool, se crea una nueva
        if (connectionPool.isEmpty()) {
            LOGGER.info("No hay conexiones disponibles.");
            return null;
        } else {
            // Si hay conexiones disponibles, se toma una del pool
            LOGGER.log(Level.INFO, "Conexiones disponibles: {0}", connectionPool.size());
            return connectionPool.pop();
        }
    }

    /**
     * Método sincronizado para liberar una conexión y devolverla al pool.
     *
     * @param connection la conexión a devolver al pool.
     */
    public synchronized void releaseConnection(Connection connection) {
        // Añadir la conexión de vuelta al pool
        connectionPool.push(connection);
        LOGGER.log(Level.INFO, "Conexión liberada de vuelta al pool. Quedan: {0}", connectionPool.size());
    }

    @Override
    public synchronized void close() {
        // Cierra todas las conexiones en el pool
        while (!connectionPool.isEmpty()) {
            Connection connection = connectionPool.pop();
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                    LOGGER.info("Conexión cerrada.");
                }
            } catch (SQLException event) {
                LOGGER.log(Level.WARNING, "Error al cerrar la conexión: {0}", event.getMessage());
            }
        }
        LOGGER.info("Todas las conexiones han sido cerradas.");
    }
}
