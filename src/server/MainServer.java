package server;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import utilidades.Closeable;

/**
 * Clase que representa un servidor básico multihilo.
 *
 * <p>
 * Este servidor acepta conexiones de clientes en un puerto determinado y lanza
 * un nuevo hilo para cada cliente, permitiendo que varios clientes se conecten
 * al mismo tiempo. Incluye un mecanismo para detectar la tecla ENTER y detener
 * el servidor cuando sea necesario.
 *
 * @author Sergio
 */
public class MainServer {

    /**
     * Logger para registrar eventos y errores del servidor.
     */
    private static final Logger LOGGER = Logger.getLogger(MainServer.class.getName());

    /**
     * Puerto en el que el servidor escuchará conexiones de clientes.
     */
    private final int PORT;

    /**
     * Variable de control para mantener el estado activo/inactivo del servidor.
     */
    private boolean running = true;

    /**
     * Objeto Closeable para manejar el pool de conexiones.
     */
    private Closeable pool;

    /**
     * Lista sincronizada para almacenar los hilos de cada cliente conectado.
     */
    List<Thread> threadsList;

    /**
     * Constructor que inicializa el servidor con el puerto especificado.
     *
     * @param PORT el puerto en el que el servidor escuchará
     */
    public MainServer(int PORT) {
        this.PORT = PORT;
        threadsList = Collections.synchronizedList(new ArrayList<>());
    }

    /**
     * Inicia el servidor, permitiendo aceptar conexiones de clientes en el
     * puerto configurado.
     *
     * <p>
     * También lanza un hilo que detecta cuando se presiona ENTER para detener
     * el servidor.
     */
    public void init() {

        // Inicia el detector de ENTER para detener el servidor con esta tecla
        KeyPressDetector detector = new KeyPressDetector(this);
        Thread exitThread = new Thread(detector);
        exitThread.start();

        // Usamos un bloque try-with-resources para asegurar el cierre del ServerSocket
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            LOGGER.info("Servidor iniciado en el puerto " + PORT);

            // Bucle que sigue aceptando clientes mientras el servidor esté en ejecución
            while (running) {
                // Espera y acepta la conexión de un cliente
                Socket socketClient = serverSocket.accept();
                LOGGER.info("Cliente conectado desde: " + socketClient.getInetAddress());

                // Crea y lanza un nuevo hilo para manejar al cliente
                Worker worker = new Worker(socketClient);
                Thread thread = new Thread(worker);
                threadsList.add(thread);
                thread.start();
            }
        } catch (Exception event) {
            LOGGER.warning("Error al crear Server Socket: " + event.getMessage());
        } finally {
            stopServer(); // Detiene el servidor cuando termina el try
            LOGGER.info("Servidor parado");
        }
    }

    /**
     * Detiene el servidor y cierra todas las conexiones activas.
     *
     * <p>
     * Interrumpe y espera a que todos los hilos de clientes terminen.
     */
    public void stopServer() {
        running = false;

        // Intenta interrumpir todos los hilos activos
        for (Thread thread : threadsList) {
            if (thread.isAlive()) {
                thread.interrupt();
            }
        }

        // Espera a que cada hilo termine antes de cerrar el servidor
        for (Thread thread : threadsList) {
            try {
                thread.join(); // Espera a que el hilo finalice
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restablece el estado de interrupción
            }
        }

        // Cierra el pool de conexiones si está en uso
        if (pool != null) {
            pool.close();
        }
    }

    /**
     * Método principal para iniciar el servidor.
     *
     * <p>
     * Carga el puerto desde un archivo de configuración y lanza el servidor.
     *
     */
    public static void main(String[] args) {
        // Carga las propiedades desde el archivo dbserver.dbConnection
        ResourceBundle bundle = ResourceBundle.getBundle("dbserver.dbConnection");
        MainServer server = new MainServer(Integer.parseInt(bundle.getString("db.port")));

        // Inicia el servidor en el puerto configurado
        server.init();
    }
}
