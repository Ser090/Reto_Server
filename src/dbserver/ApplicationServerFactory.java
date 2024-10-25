package dbserver;

import java.util.ResourceBundle;
import java.util.logging.Logger;
import utilidades.Closeable;
import utilidades.Signable;

/**
 *
 * @author Sergio
 */
public class ApplicationServerFactory {

    // Logger para registrar eventos y errores
    private static final Logger LOGGER = Logger.getLogger(ApplicationServerFactory.class.getName());
    private Dao dao;
    // Instancia única de ApplicationServerFactory (patrón Singleton)
    private static ApplicationServerFactory instance;

    ResourceBundle bundle = ResourceBundle.getBundle("dbserver.dbConnection");

    // Pool de conexiones a la base de datos Postgres
    private PostgresConnectionPool pool;

    // Constructor que inicializa el pool de conexiones con un tamaño de 10 conexiones
    public ApplicationServerFactory() {
        pool = new PostgresConnectionPool(Integer.parseInt(bundle.getString("db.poolSize")));
        dao = new Dao(pool);

    }

    // Método para obtener la única instancia de ApplicationServerFactory (Singleton)
    public static synchronized ApplicationServerFactory getInstance() {
        // Si la instancia aún no existe, la crea
        if (instance == null) {
            instance = new ApplicationServerFactory();
        }
        // Devuelve la instancia existente
        return instance;
    }

    public Signable acceso() {
        return dao;
    }

    public Closeable cerrar() {
        return pool;

    }

}
