package server;

import dbserver.ApplicationServerFactory;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.logging.Logger;
import utilidades.Message;
import utilidades.MessageType;
import utilidades.User;

/**
 *
 * @author Sergio
 */
public class Worker implements Runnable {

    // Logger para registrar eventos y errores
    private static final Logger LOGGER = Logger.getLogger(Worker.class.getName());

    // Socket del cliente con el que el servidor se comunicará
    private Socket clienteSocket;

    // Streams para enviar y recibir objetos
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;

    // Constructor que inicializa el socket y el DAO SOBRA
    public Worker(Socket clienteSocket) {
        this.clienteSocket = clienteSocket;
    }

    // Método principal que se ejecuta en el hilo para manejar la conexión del cliente
    @Override
    public void run() {
        try {
            // Inicializa los streams de entrada y salida para la comunicación con el cliente
            outputStream = new ObjectOutputStream(clienteSocket.getOutputStream());
            inputStream = new ObjectInputStream(clienteSocket.getInputStream());

            // Lee el objeto enviado por el cliente
            Object mensajeObject = inputStream.readObject();

            // Verifica si el objeto recibido es un Message
            if (mensajeObject instanceof Message) {
                Message mensaje = (Message) mensajeObject;
                procesarMensaje(mensaje); // Procesa el mensaje recibido
            }

        } catch (IOException | ClassNotFoundException ex) {
            // Maneja errores de E/S o cuando la clase no es encontrada
            if (ex instanceof IOException) {
                LOGGER.warning("Fallo en la lectura del fichero");
            } else {
                LOGGER.warning("Clase no encontrada");
            }
        } finally {
            // Asegura el cierre de la conexión
            cerrarConexion();
        }
    }

    // METODOS PRIVADOS
    // Procesa el mensaje recibido del cliente y responde adecuadamente
    private void procesarMensaje(Message mensaje) {
        Message respuesta;
        // Obtiene el objeto User del mensaje recibido
        User user = (User) mensaje.getObject();

        // Verifica si el usuario recibido es nulo
        if (user == null) {
            respuesta = new Message(MessageType.BAD_RESPONSE, null); // Respuesta negativa
        } else {
            // Determina el tipo de mensaje (sign up o sign in) y actúa en consecuencia
            switch (mensaje.getType()) {
                case SIGN_UP_REQUEST:
                    respuesta = ApplicationServerFactory.getInstance().acceso().signUp(user);
                    break;
                case SIGN_IN_REQUEST:
                    respuesta = ApplicationServerFactory.getInstance().acceso().signIn(user);
                    break;
                default:
                    respuesta = new Message(MessageType.BAD_RESPONSE, user); // Respuesta para tipo desconocido
            }
        }
        enviarRespuesta(respuesta); // Envía la respuesta al cliente
    }

    // Envía la respuesta al cliente a través del output stream
    private void enviarRespuesta(Message respuesta) {
        try {
            // Escribe la respuesta en el stream de salida
            outputStream.writeObject(respuesta);
            outputStream.flush(); // Asegura que se envíen los datos
        } catch (IOException e) {
            LOGGER.warning("Fallo en la lectura o escritura del fichero");
            new Message(MessageType.BAD_RESPONSE, e); // Respuesta negativa en caso de error
        }
    }

    // Cierra la conexión con el cliente
    private void cerrarConexion() {
        try {
            // Si el socket no es nulo, lo cierra
            if (clienteSocket != null) {
                clienteSocket.close();
            }
        } catch (Exception e) {
            LOGGER.severe("Fallo en el cierre del server");
            new Message(MessageType.BAD_RESPONSE, e); // Mensaje de error si el cierre falla
        }
    }
}
