package dbserver;

import java.net.Socket;
import java.util.logging.Logger;
import server.Worker;

/**
 *
 * @author Sergio
 */
public class ApplicationServerFactory {

    // Logger para registrar eventos y errores
    private static final Logger LOGGER = Logger.getLogger(ApplicationServerFactory.class.getName());

    // Instancia única de ApplicationServerFactory (patrón Singleton)
    private static ApplicationServerFactory instance;

    // Pool de conexiones a la base de datos Postgres
    private PostgresConnectionPool pool;

    // Constructor que inicializa el pool de conexiones con un tamaño de 10 conexiones
    public ApplicationServerFactory() {
        pool = new PostgresConnectionPool(10);
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

    // Método que crea y devuelve un Worker, inyectando un DAO que utiliza el pool de conexiones
    public Worker crearWorker(Socket clienteSocket) {
        Dao dao = new Dao(pool); // Crea el DAO con el pool de conexiones
        return new Worker(clienteSocket, dao); // Crea el Worker pasándole el socket y el DAO
    }
}
