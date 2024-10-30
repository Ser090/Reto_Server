/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

import java.io.IOException;
import java.util.logging.Logger;

/**
 *
 * @author Sergio
 */
public class KeyPressDetector implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(KeyPressDetector.class.getName());
    private boolean stop = false;
    private final MainServer SERVER;

    public KeyPressDetector(MainServer SERVER) {
        this.SERVER = SERVER;
    }


    @Override
    public void run() {
        try {
            while (!stop) {
                // Espera a que el evento suceda
                int keyCode = System.in.read();
                if (keyCode == 10) { // ENTER key
                    LOGGER.info("Tecla <enter> detectada. Saliendo...");
                    SERVER.detener(); // Detener el servidor
                    LOGGER.info("Servidor Parado");
                    System.exit(0);
                }
            }
        } catch (IOException e) {
            LOGGER.severe("Error al leer la entrada: " + e.getMessage());
        }
    }

    public void stop() {
        stop = true;
    }
}
