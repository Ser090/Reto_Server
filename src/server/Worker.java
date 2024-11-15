package server;

import dbserver.ApplicationServerFactory;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import utilidades.Message;
import utilidades.MessageType;
import utilidades.User;

/**
 * Clase Worker que maneja la comunicación con un cliente conectado al servidor.
 *
 * <p>
 * Esta clase se ejecuta en un hilo separado por cada cliente y se encarga de
 * recibir y procesar mensajes, así como de enviar respuestas. Trabaja con
 * objetos de tipo Message para coordinar acciones como registro o inicio de
 * sesión de usuarios.
 *
 * @author Sergio
 */
public class Worker implements Runnable {

    /**
     * Logger para registrar eventos y errores durante la comunicación.
     */
    private static final Logger LOGGER = Logger.getLogger(Worker.class.getName());

    /**
     * Socket del cliente conectado al servidor.
     */
    private Socket socketClient;

    /**
     * Stream de entrada para recibir objetos del cliente.
     */
    private ObjectInputStream inputStream;

    /**
     * Stream de salida para enviar objetos al cliente.
     */
    private ObjectOutputStream outputStream;

    /**
     * Constructor que inicializa el socket de cliente.
     *
     * @param socketClient el socket del cliente con el que el servidor se
     * comunicará
     */
    public Worker(Socket socketClient) {
        this.socketClient = socketClient;
    }

    /**
     * Método principal del hilo que establece la comunicación con el cliente.
     *
     * <p>
     * Inicializa los streams de entrada y salida, recibe el mensaje y lo
     * procesa según el tipo de solicitud del cliente.
     */
    @Override
    public void run() {
        try {
            // Inicializa los streams para enviar y recibir objetos
            outputStream = new ObjectOutputStream(socketClient.getOutputStream());
            inputStream = new ObjectInputStream(socketClient.getInputStream());

            // Lee el objeto enviado por el cliente
            Object objectMessage = inputStream.readObject();

            // Verifica si el objeto recibido es del tipo Message
            if (objectMessage instanceof Message) {
                Message message = (Message) objectMessage;
                processMessage(message); // Procesa el mensaje recibido
            }

        } catch (IOException | ClassNotFoundException event) {
            // Manejo de excepciones para errores de E/S o clases no encontradas
            if (event instanceof IOException) {
                LOGGER.severe("Fallo en la lectura del archivo: " + event.getMessage());
            } else {
                LOGGER.severe("Clase no encontrada: " + event.getMessage());
            }
        } finally {
            // Cierra la conexión después de terminar la comunicación
            closeConnection();
        }
    }

    // METODOS PRIVADOS
    /**
     * Procesa el mensaje recibido del cliente y responde según el tipo de
     * solicitud.
     *
     * @param message el mensaje recibido, que contiene el tipo de solicitud y
     * datos del usuario
     */
    private void processMessage(Message message) {
        Message response;
        // Extrae el objeto User del mensaje
        User user = (User) message.getObject();

        // Verifica si el usuario no es nulo antes de procesar el mensaje
        if (user == null) {
            response = new Message(MessageType.BAD_RESPONSE, null); // Respuesta negativa
        } else {
            // Procesa la solicitud según el tipo de mensaje (registro o inicio de sesión)
            switch (message.getType()) {
                case SIGN_UP_REQUEST:
                    response = ApplicationServerFactory.getInstance().access().signUp(user);
                    break;
                case SIGN_IN_REQUEST:
                    response = ApplicationServerFactory.getInstance().access().signIn(user);
                    break;
                case GET_USER:
                    response = ApplicationServerFactory.getInstance().access().getUser(user);
                default:
                    response = new Message(MessageType.BAD_RESPONSE, user); // Respuesta para tipo desconocido
            }
        }
        sendResponse(response); // Envía la respuesta al cliente
    }

    /**
     * Envía la respuesta al cliente.
     *
     * @param response el mensaje de respuesta a enviar al cliente
     */
    private void sendResponse(Message response) {
        try {
            // Envía el objeto de respuesta y asegura que se envíen los datos
            outputStream.writeObject(response);
            outputStream.flush();
        } catch (IOException event) {
            LOGGER.log(Level.SEVERE, "Fallo en la lectura o escritura del archivo: {0}", event.getMessage());
            new Message(MessageType.BAD_RESPONSE, event); // Envia respuesta negativa en caso de error
        }
    }

    /**
     * Cierra la conexión con el cliente y los streams.
     *
     * <p>
     * Asegura que los recursos se liberen adecuadamente después de la
     * comunicación.
     */
    private void closeConnection() {
        try {
            // Cierra el stream de entrada si está inicializado
            if (inputStream != null) {
                inputStream.close();
            }
            // Cierra el stream de salida si está inicializado
            if (outputStream != null) {
                outputStream.close();
            }
            // Cierra el socket del cliente si está abierto
            if (socketClient != null && !socketClient.isClosed()) {
                socketClient.close();
                LOGGER.info("Conexión cerrada con el cliente.");
            }
        } catch (IOException event) {
            LOGGER.log(Level.SEVERE, "Fallo al cerrar el servidor: {0}", event.getMessage());
            new Message(MessageType.BAD_RESPONSE, event); // Envia un mensaje de error si falla el cierre
        }
    }
}
