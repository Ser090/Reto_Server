package dbserver;

import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import utilidades.Closeable;
import utilidades.Signable;

/**
 * Clase ApplicationServerFactory que implementa el patrón Singleton para
 * gestionar la conexión a la base de datos y la creación de DAOs.
 *
 * <p>
 * Esta clase proporciona acceso a una instancia única de {@code Dao} y maneja
 * el pool de conexiones a la base de datos PostgreSQL.
 * </p>
 *
 * <p>
 * Utiliza un archivo de propiedades para definir el tamaño del pool de
 * conexiones. Si el valor no es válido o no está presente, el tamaño
 * predeterminado se establece en {@code 0}.
 * </p>
 *
 * <p>
 * En caso de error al obtener el tamaño del pool, se utiliza el valor
 * predeterminado configurado en el atributo {@code poolSize}.
 * </p>
 *
 * @author Sergio
 */
public class ApplicationServerFactory {

    /**
     * Logger para registrar eventos y errores.
     */
    private static final Logger LOGGER = Logger.getLogger(ApplicationServerFactory.class.getName());

    /**
     * Objeto {@code Dao} para gestionar operaciones en la base de datos.
     */
    private Dao dao;

    /**
     * Tamaño del pool de conexiones, obtenido del archivo de configuración. Si
     * no se encuentra el valor o es inválido, se establece un valor por
     * defecto.
     */
    private Integer poolSize;

    /**
     * Instancia única de {@code ApplicationServerFactory} (patrón Singleton).
     */
    private static ApplicationServerFactory instance;

    /**
     * Pool de conexiones a la base de datos PostgreSQL.
     */
    private PostgresConnectionPool connectionPool;

    /**
     * Constructor que inicializa el pool de conexiones con un tamaño definido
     * en el archivo de configuración.
     *
     * <p>
     * Intenta obtener el tamaño del pool de conexiones desde un archivo de
     * propiedades. En caso de que falte el valor o esté mal formateado,
     * registra el error y asigna un tamaño predeterminado de {@code 0}.
     * </p>
     */
    public ApplicationServerFactory() {
        try {
            ResourceBundle resourceBundle = ResourceBundle.getBundle("dbserver.dbConnection");
            // Inicializa el pool de conexiones utilizando el tamaño especificado en el bundle
            poolSize = Integer.parseInt(resourceBundle.getString("db.poolSize"));

        } catch (MissingResourceException event) {
            LOGGER.log(Level.SEVERE, "El poolSize no se encuentra en el archivo: {0}", event.getMessage());
            connectionPool = new PostgresConnectionPool(0);
        } catch (NumberFormatException event) {
            LOGGER.log(Level.SEVERE, "El tamaño de pool es inválido o está mal formateado: {0}", event.getMessage());
            // Asigna un tamaño predeterminado si el valor no es numérico
            connectionPool = new PostgresConnectionPool(0);
        }
        connectionPool = new PostgresConnectionPool(poolSize);
        dao = new Dao(connectionPool);
    }

    /**
     * Método para obtener la única instancia de
     * {@code ApplicationServerFactory}.
     *
     * <p>
     * Este método asegura que solo haya una instancia de la clase (Singleton) y
     * la devuelve. Si la instancia no existe, se crea una nueva.
     * </p>
     *
     * @return La instancia única de {@code ApplicationServerFactory}.
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
     * Proporciona acceso al objeto {@code Dao} para realizar operaciones en la
     * base de datos.
     *
     * @return Objeto que implementa la interfaz {@code Signable} para gestionar
     * el acceso a la base de datos.
     */
    public Signable access() {
        return dao;
    }

    /**
     * Cierra el pool de conexiones a la base de datos.
     *
     * @return Objeto que implementa la interfaz {@code Closeable} para
     * gestionar el cierre del pool.
     */
    public Closeable close() {
        return connectionPool;
    }
}
