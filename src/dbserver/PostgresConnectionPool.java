package dbserver;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;
import utilidades.Closeable;

/**
 * Clase que gestiona un pool de conexiones a una base de datos PostgreSQL.
 *
 * <p>
 * Esta clase permite reutilizar conexiones a la base de datos mediante un pool,
 * optimizando el uso de recursos. Implementa la interfaz {@code Closeable} para
 * garantizar el cierre controlado de todas las conexiones activas.
 *
 * <p>
 * El pool se configura mediante un archivo de propiedades y un parámetro de
 * tamaño, creando un número inicial de conexiones que luego se reutilizan. Si
 * no hay conexiones disponibles al momento de solicitar una, el método
 * {@link #getConnection()} devolverá {@code null}.
 *
 * <p>
 * Esta implementación es segura para hilos, permitiendo acceso concurrente a
 * las conexiones del pool.
 *
 * @author Urko
 */
public class PostgresConnectionPool implements Closeable {

    /**
     * Logger para registrar eventos y errores de la clase.
     */
    private static final Logger LOGGER = Logger.getLogger(PostgresConnectionPool.class.getName());

    /**
     * Almacena las conexiones disponibles en el pool.
     */
    private Stack<Connection> connectionPool = new Stack<>();

    /**
     * Variables de configuración de conexión a la base de datos.
     */
    private String url;
    private String user;
    private String password;

    /**
     * Crea un pool de conexiones y carga las propiedades de conexión desde un
     * archivo de configuración.
     *
     * <p>
     * Crea el número de conexiones inicial especificado en {@code poolSize}. Si
     * el pool no puede conectar una instancia, se registra un mensaje de
     * advertencia.
     *
     * @param poolSize Número de conexiones a crear en el pool.
     */
    public PostgresConnectionPool(int poolSize) {
        // Cargar los datos de conexión desde el archivo de propiedades
        loadProperties();

        try {
            // Cargar el driver de PostgreSQL
            Class.forName("org.postgresql.Driver");

            // Crear conexiones según poolSize
            for (int i = 0; i < poolSize; i++) {
                Connection conn = DriverManager.getConnection(url, user, password);
                if (conn != null) {
                    LOGGER.log(Level.INFO, "Conexión {0} creada y añadida al pool.", i + 1);
                    connectionPool.push(conn);
                } else {
                    LOGGER.log(Level.WARNING, "No se ha podido crear la conexión {0}", i + 1);
                }
            }
        } catch (SQLException event) {
            LOGGER.warning("Error de conexión a la base de datos.");
        } catch (ClassNotFoundException event) {
            LOGGER.warning("Driver no encontrado.");
        }
    }

    /**
     * Carga las propiedades de conexión desde el archivo de configuración
     * {@code dbserver.dbConnection}.
     *
     * <p>
     * Asigna a las variables de instancia los valores de URL, usuario y
     * contraseña. Si el archivo o propiedades no se encuentran, se registra un
     * error severo.
     */
    private void loadProperties() {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("dbserver.dbConnection");
            url = bundle.getString("db.url");
            user = bundle.getString("db.user");
            password = bundle.getString("db.password");
        } catch (MissingResourceException event) {
            LOGGER.log(Level.SEVERE, "Los parámetros de conexión no se encuentran {0}", event.getMessage());
        }
    }

    /**
     * Obtiene una conexión del pool de conexiones.
     *
     * <p>
     * Este método es sincronizado para garantizar un acceso seguro en entornos
     * multihilo. Si no hay conexiones disponibles, devuelve {@code null} y
     * registra un mensaje informativo.
     *
     * @return una conexión disponible o {@code null} si no hay conexiones en el
     * pool.
     * @throws SQLException si ocurre un error al obtener la conexión.
     */
    public synchronized Connection getConnection() throws SQLException {
        if (connectionPool.isEmpty()) {
            LOGGER.info("No hay conexiones disponibles.");
            return null;
        } else {
            LOGGER.log(Level.INFO, "Conexiones disponibles: {0}", connectionPool.size());
            return connectionPool.pop();
        }
    }

    /**
     * Libera una conexión devolviéndola al pool.
     *
     * <p>
     * Este método es sincronizado y permite que las conexiones se liberen
     * correctamente para evitar fugas de conexión y maximizar la reutilización.
     *
     * @param connection la conexión a devolver al pool.
     */
    public synchronized void releaseConnection(Connection connection) {
        connectionPool.push(connection);
        LOGGER.log(Level.INFO, "Conexión liberada al pool. Quedan: {0}", connectionPool.size());
    }

    /**
     * Cierra todas las conexiones del pool.
     *
     * <p>
     * Este método es sincronizado y cierra todas las conexiones aún abiertas en
     * el pool, garantizando que todos los recursos se liberen adecuadamente.
     * Las excepciones de cierre se registran en el log.
     */
    @Override
    public synchronized void close() {
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
