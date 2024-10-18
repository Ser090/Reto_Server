package server;

import dbserver.ApplicationServerFactory;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Logger;

/**
 *
 * @author Sergio
 */
public class MainServer {

    // Instancia del logger para registrar información y errores en el servidor
    private static final Logger LOGGER = Logger.getLogger(MainServer.class.getName());

    // Puerto en el que se iniciará el servidor
    private final int puerto;

    // Control para detener el servidor
    private volatile boolean running = true;

    // Constructor que inicializa el puerto del servidor
    public MainServer(int puerto) {
        this.puerto = puerto;
    }

    // Método para iniciar el servidor
    public void iniciar() {

        //Implementacion de del detector de ENTER para parar el server
        KeyPressDetector detector = new KeyPressDetector(this);
        Thread exitThread = new Thread(detector);
        exitThread.start();

        // El bloque try-with-resources asegura que el ServerSocket se cierre automáticamente
        try (ServerSocket serverSocket = new ServerSocket(puerto)) {

            // Log para indicar que el servidor ha iniciado en el puerto especificado
            LOGGER.info("Servidor iniciado en el puerto " + puerto);

            // Bucle infinito para aceptar conexiones de clientes
            while (running) {

                // Acepta la conexión de un cliente
                Socket clienteSocket = serverSocket.accept();

                // Log para indicar que un cliente se ha conectado, mostrando su dirección IP
                LOGGER.info("Cliente conectado desde: " + clienteSocket.getInetAddress());

                // Crea un Worker para manejar la conexión del cliente
                Worker worker = ApplicationServerFactory.getInstance().crearWorker(clienteSocket);

                // Inicia un nuevo hilo para manejar la comunicación con el cliente
                new Thread(worker).start();
            }
        } catch (Exception e) {
            // Log de advertencia en caso de error al crear el ServerSocket
            LOGGER.warning("Error al crear Server Socket.");
        }
    }

    // Método para detener el servidor
    public void detener() {
        running = false;
        //Aqui gestionamos la parada del pool
    }


    // Método principal que inicia el servidor en el puerto 1234
    public static void main(String[] args) {
        // Crea una instancia de MainServer con el puerto 1234
        MainServer servidor = new MainServer(1234);

        // Inicia el servidor
        servidor.iniciar();
    }
}
