package server;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Clase que detecta la tecla ENTER para detener el servidor. Cuando el usuario
 * presiona ENTER, el servidor se detiene y se cierra la aplicación.
 *
 * Esta clase se ejecuta en un hilo aparte y revisa continuamente si se ha
 * presionado ENTER.
 *
 * @author Sergio
 */
public class KeyPressDetector implements Runnable {

    /**
     * Registro para mostrar mensajes de log
     */
    private static final Logger LOGGER = Logger.getLogger(KeyPressDetector.class.getName());

    /**
     * Controla si el bucle de detección sigue activo
     */
    private boolean stop = false;

    /**
     * Referencia al servidor principal para detenerlo
     */
    private final MainServer SERVER;

    /**
     * Constructor de la clase, asigna el servidor que va a detener.
     *
     * @param SERVER instancia del servidor principal
     */
    public KeyPressDetector(MainServer SERVER) {
        this.SERVER = SERVER;
    }

    /**
     * Método principal que escucha la tecla ENTER. Si se presiona ENTER, se
     * detiene el servidor y se cierra la aplicación.
     */
    @Override
    public void run() {
        try {
            while (!stop) {
                // Lee la entrada de teclado
                int keyCode = System.in.read();

                // Si se detecta ENTER (código 10)
                if (keyCode == 10) {
                    LOGGER.info("Tecla <enter> detectada. Saliendo...");
                    SERVER.detener(); // Detiene el servidor
                    LOGGER.info("Servidor Parado");
                    System.exit(0); // Termina el programa
                }
            }
        } catch (IOException event) {
            // Muestra un error en caso de problemas de lectura
            LOGGER.severe("Error al leer la entrada: " + event.getMessage());
        }
    }

    /**
     * Cambia el valor de stop a true para finalizar el bucle de detección.
     */
    public void stop() {
        stop = true;
    }
}
