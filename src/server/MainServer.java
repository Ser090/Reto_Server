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
 *
 * @author Sergio
 */
public class MainServer {

    // Instancia del logger para registrar información y errores en el servidor
    private static final Logger LOGGER = Logger.getLogger(MainServer.class.getName());

    // Puerto en el que se iniciará el servidor
    private final int PORT;

    // Control para detener el servidor
    private boolean running = true;

    private Closeable pool;

    List<Thread> threadsList;

    // Constructor que inicializa el puerto del servidor
    public MainServer(int PORT) {
        this.PORT = PORT;
        threadsList = Collections.synchronizedList(new ArrayList<>());
    }

    // Método para iniciar el servidor
    public void iniciar() {

        //Implementacion de del detector de ENTER para parar el server
        KeyPressDetector detector = new KeyPressDetector(this);
        Thread exitThread = new Thread(detector);
        exitThread.start();

        // El bloque try-with-resources asegura que el ServerSocket se cierre automáticamente
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {

            // Log para indicar que el servidor ha iniciado en el puerto especificado
            LOGGER.info("Servidor iniciado en el puerto " + PORT);

            // Bucle infinito para aceptar conexiones de clientes
            while (running) {

                // Acepta la conexión de un cliente
                Socket clienteSocket = serverSocket.accept();

                // Log para indicar que un cliente se ha conectado, mostrando su dirección IP
                LOGGER.info("Cliente conectado desde: " + clienteSocket.getInetAddress());
                Worker worker = new Worker(clienteSocket);
                Thread thread = new Thread(worker);
                threadsList.add(thread);
                thread.start();
            }
        } catch (Exception e) {
            // Log de advertencia en caso de error al crear el ServerSocket
            LOGGER.warning("Error al crear Server Socket." + e.getMessage());
        } finally {
            detener();
            LOGGER.info("Servidor parado");
        }
    }

    // Método para detener el servidor
    public void detener() {
        running = false;

        for (Thread thread : threadsList) {
            if (thread.isAlive()) {
                thread.interrupt(); // Interrumpe el hilo si está vivo
            }
        }

        // Luego espera a que todos los hilos terminen
        for (Thread thread : threadsList) {
            try {
                thread.join(); // Espera a que el hilo termine
            } catch (InterruptedException e) {
                // Manejar la excepción si el hilo actual es interrumpido
                Thread.currentThread().interrupt(); // Restablece el estado de interrupción
            }
        }

        if (pool != null) {
            pool.close();
        }

    }
    // Método principal que inicia el servidor en el puerto 1234

    public static void main(String[] args) {
        ResourceBundle bundle = ResourceBundle.getBundle("dbserver.dbConnection");
        // Crea una instancia de MainServer con el puerto 1234
        MainServer servidor = new MainServer(Integer.parseInt(bundle.getString("db.port")));

        // Inicia el servidor
        servidor.iniciar();
    }
}
