/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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

    private static final Logger LOGGER = Logger.getLogger(MainServer.class.getName());
    private final int puerto;

    public MainServer(int puerto) {
        this.puerto = puerto;
    }

    public void iniciar() {
        try (ServerSocket serverSocket = new ServerSocket(puerto)) {
            LOGGER.info("Servidor iniciado en el puerto " + puerto);
            while (true) {
                Socket clienteSocket = serverSocket.accept();
                LOGGER.info("Cliente conectado desde: " + clienteSocket.getInetAddress());
                Worker worker = ApplicationServerFactory.getInstance().crearWorker(clienteSocket);
                new Thread(worker).start();
            }
        } catch (Exception e) {
            LOGGER.warning("Error al crear Server Socket.");
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        MainServer servidor = new MainServer(1234);
        servidor.iniciar();
    }

}
