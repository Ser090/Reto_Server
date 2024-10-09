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
import java.util.logging.Level;
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
                Logger.getLogger(Worker.class.getName()).log(Level.SEVERE, null, ex);
            } else {
                Logger.getLogger(Worker.class.getName()).log(Level.SEVERE, null, ex);
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
        User user = (User) mensaje.getObject();
        if (user == null) {
            respuesta = new Message(MessageType.BAD_RESPONSE, user);
        } else {
            switch (mensaje.getType()) {
                case COUNTRIES_REQUEST:
                    respuesta = getCountries();
                    break;
                default:
                    respuesta = new Message(MessageType.BAD_RESPONSE, user);
            }
        }
        enviarRespuesta(respuesta);

    }

    private void enviarRespuesta(Message respuesta) {
        try {
            outputStream.writeObject(respuesta);
            outputStream.flush();
        } catch (IOException e) {
            new Message(MessageType.BAD_RESPONSE, e);
        }
    }

    private void cerrarConexion() {
        try {
            if (clienteSocket != null) {
                clienteSocket.close();
            }
        } catch (Exception e) {
            new Message(MessageType.BAD_RESPONSE, e);
        }
    }

}
