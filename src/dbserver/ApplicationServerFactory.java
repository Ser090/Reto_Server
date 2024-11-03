package dbserver;

import java.util.ResourceBundle;
import java.util.logging.Logger;
import utilidades.Closeable;
import utilidades.Signable;

/**
 * Clase ApplicationServerFactory que implementa el patrón Singleton para
 * gestionar la conexión a la base de datos y la creación de DAOs.
 *
 * <p>
 * Esta clase proporciona acceso a una instancia única de Dao y maneja el pool
 * de conexiones a la base de datos PostgreSQL.
 *
 * @author Sergio
 */
public class ApplicationServerFactory {

    /**
     * Logger para registrar eventos y errores.
     */
    private static final Logger LOGGER = Logger.getLogger(ApplicationServerFactory.class.getName());

    /**
     * Objeto Dao para gestionar operaciones en la base de datos.
     */
    private Dao dao;

    /**
     * Instancia única de ApplicationServerFactory (patrón Singleton).
     */
    private static ApplicationServerFactory instance;

    /**
     * Recurso de configuración para la conexión a la base de datos.
     */
    ResourceBundle resourceBundle = ResourceBundle.getBundle("dbserver.dbConnection");

    /**
     * Pool de conexiones a la base de datos Postgres.
     */
    private PostgresConnectionPool connectionPool;

    /**
     * Constructor que inicializa el pool de conexiones con un tamaño definido
     * en la configuración.
     *
     * <p>
     * Se crea una instancia del pool de conexiones y un Dao asociado.
     */
    public ApplicationServerFactory() {
        // Inicializa el pool de conexiones utilizando el tamaño especificado en el bundle
        connectionPool = new PostgresConnectionPool(Integer.parseInt(resourceBundle.getString("db.poolSize")));
        dao = new Dao(connectionPool);
    }

    /**
     * Método para obtener la única instancia de ApplicationServerFactory.
     *
     * <p>
     * Este método asegura que solo haya una instancia de la clase (Singleton) y
     * la devuelve. Si la instancia no existe, se crea una nueva.
     *
     * @return La instancia única de ApplicationServerFactory.
     */
    public static synchronized ApplicationServerFactory getInstance() {
        // Si la instancia aún no existe, la crea
        if (instance == null) {
            instance = new ApplicationServerFactory();
        }
        // Devuelve la instancia existente
        return instance;
    }

    /**
     * Proporciona acceso al objeto Dao para realizar operaciones en la base de
     * datos.
     *
     * @return Objeto que implementa la interfaz Signable para gestionar el
     * acceso a la base de datos.
     */
    public Signable access() {
        return dao;
    }

    /**
     * Cierra el pool de conexiones a la base de datos.
     *
     * @return Objeto que implementa la interfaz Closeable para gestionar el
     * cierre del pool.
     */
    public Closeable close() {
        return connectionPool;
    }
}
