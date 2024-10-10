/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.logging.Logger;
import utilidades.Message;
import utilidades.MessageType;
import utilidades.Signable;
import utilidades.User;

/**
 *
 * @author Sergio
 */
public class Worker implements Runnable, Signable {

    private static final Logger logger = Logger.getLogger(Worker.class.getName());

    private Socket clienteSocket;
    private Signable dao;
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;

    public Worker(Socket clienteSocket, Signable dao) {
        this.clienteSocket = clienteSocket;
        this.dao = dao;
    }

    @Override
    public void run() {
        try {
            outputStream = new ObjectOutputStream(clienteSocket.getOutputStream());
            inputStream = new ObjectInputStream(clienteSocket.getInputStream());

            Object mensajeObject = inputStream.readObject();
            if (mensajeObject instanceof Message) {
                Message mensaje = (Message) mensajeObject;
                procesarMensaje(mensaje);
            }

        } catch (IOException | ClassNotFoundException ex) {
            if (ex instanceof IOException) {
                logger.warning("Fallo en la lectura del fichero");
            } else {
                logger.warning("Clase no encontrada");
            }
        } finally {
            cerrarConexion();
        }

    }

    @Override
    public Message signIn(User user) {
        return dao.signIn(user);
    }

    @Override
    public Message signUp(User user) {
        return dao.signUp(user);
    }

    @Override
    public Message getCountries() {
        return dao.getCountries();
    }

    //METODOS PRIVADOS
    private void procesarMensaje(Message mensaje) {
        Message respuesta;
        if (mensaje.getType().equals(MessageType.COUNTRIES_REQUEST)) {
            respuesta = getCountries();
        } else {
            User user = (User) mensaje.getObject();
            if (user == null) {
                respuesta = new Message(MessageType.BAD_RESPONSE, user);
            } else {
                switch (mensaje.getType()) {
                    case COUNTRIES_REQUEST:
                        respuesta = getCountries();
                        break;
                    case SIGN_UP_REQUEST:
                        respuesta = signUp(user);
                        break;
                    case SIGN_IN_REQUEST:
                        respuesta = signIn(user);
                        break;
                    default:
                        respuesta = new Message(MessageType.BAD_RESPONSE, user);
                }
            }
        }

        enviarRespuesta(respuesta);

    }

    private void enviarRespuesta(Message respuesta) {
        try {
            outputStream.writeObject(respuesta);
            outputStream.flush();
        } catch (IOException e) {
            logger.warning("Fallo en la lectura o escritura del fichero");
            new Message(MessageType.BAD_RESPONSE, e);
        }
    }

    private void cerrarConexion() {
        try {
            if (clienteSocket != null) {
                clienteSocket.close();
            }
        } catch (Exception e) {
            logger.severe("Fallo en el cierre del server");
            new Message(MessageType.BAD_RESPONSE, e);
        }
    }

}
