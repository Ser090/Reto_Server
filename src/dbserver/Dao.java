package dbserver;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import utilidades.Message;
import utilidades.MessageType;
import utilidades.Signable;
import utilidades.User;

/**
 * Clase Data Access Object (DAO) para gestionar la interacción con la base de
 * datos relacionada con usuarios. Implementa la interfaz {@link Signable} para
 * proporcionar métodos de registro e inicio de sesión.
 *
 * Esta clase utiliza un pool de conexiones a PostgreSQL para manejar las
 * conexiones de manera eficiente y segura.
 *
 * @author Urko
 */
public class Dao implements Signable {

    // Logger para registrar eventos y errores
    private static final Logger LOGGER = Logger.getLogger(Dao.class.getName());

    // Instancia del pool de conexiones a PostgreSQL
    private PostgresConnectionPool pool;

    // Consultas SQL para insertar y autenticar usuarios
    private final String sqlInsertUser = "INSERT INTO res_users(company_id, partner_id, active, login, password, notification_type) VALUES (1, ?, ?, ?, ?, ?) RETURNING id";
    private final String sqlInsertPartner = "INSERT INTO res_partner (company_id, name, display_name, street, zip, city, email) VALUES (1, ?, ?, ?, ?, ?, ?) RETURNING id";
    private final String sqlSignInVitaminado = "SELECT p.name, u.active FROM res_users u JOIN res_partner p ON u.partner_id = p.id WHERE u.login = ? AND u.password = ?";
    private final String sqlGetUser = "SELECT * FROM res_users u JOIN res_partner p ON u.partner_id = p.id WHERE u.login = ? AND u.password = ?";

    /**
     * Constructor que inicializa el DAO con un pool de conexiones.
     *
     * @param pool El pool de conexiones que se usará para las operaciones de
     * base de datos.
     */
    public Dao(PostgresConnectionPool pool) {
        this.pool = pool;
    }

    /**
     * Método para registrar un nuevo usuario.
     *
     * Este método realiza las siguientes acciones: 1. Obtiene una conexión del
     * pool. 2. Inserta un nuevo registro en la tabla 'res_partner'. 3. Inserta
     * un nuevo registro en la tabla 'res_users' usando el ID del registro de
     * 'res_partner'. 4. Maneja transacciones para asegurar la integridad de los
     * datos.
     *
     * @param user El objeto User que contiene la información del nuevo usuario.
     * @return Un objeto Message que indica el resultado de la operación.
     */
    @Override
    public Message signUp(User user) {

        Connection conn = null;
        PreparedStatement stmtInsertPartner = null;
        PreparedStatement stmtInsertUser = null;
        ResultSet rs = null;

        try {
            // Obtener una conexión del pool de conexiones
            conn = pool.getConnection();

            // Verificar si la conexión es válida
            if (conn == null || !conn.isValid(2)) {
                LOGGER.warning("Error: No se pudo obtener una conexión válida.");
                return new Message(MessageType.CONNECTION_ERROR, user);
            }

            // Desactivar el autocommit para manejar la transacción manualmente
            conn.setAutoCommit(false);

            // Preparar e insertar en la tabla res_partner
            stmtInsertPartner = conn.prepareStatement(sqlInsertPartner);
            stmtInsertPartner.setString(1, user.getName());      // nombre
            stmtInsertPartner.setString(2, user.getName());      // display_name
            stmtInsertPartner.setString(3, user.getStreet());    // street
            stmtInsertPartner.setString(4, user.getZip());       // zip
            stmtInsertPartner.setString(5, user.getCity());      // city
            stmtInsertPartner.setString(6, user.getLogin());     // email

            // Ejecutar la consulta e insertar en res_partner
            rs = stmtInsertPartner.executeQuery();
            if (rs.next()) {
                int resPartnerId = rs.getInt("id");

                // Preparar e insertar en res_users usando el ID de res_partner
                stmtInsertUser = conn.prepareStatement(sqlInsertUser);
                stmtInsertUser.setInt(1, resPartnerId);           // partner_id
                stmtInsertUser.setBoolean(2, user.getActive());   // active
                stmtInsertUser.setString(3, user.getLogin());     // login
                stmtInsertUser.setString(4, user.getPass());      // password
                stmtInsertUser.setString(5, "Email");             // notification_type

                // Ejecutar la consulta y obtener el ID del res_user
                rs = stmtInsertUser.executeQuery();
                if (rs.next()) {
                    int resUserId = rs.getInt("id");
                    user.setResUserId(resUserId);  // Asignar el ID generado al usuario

                    // Confirmar la transacción
                    conn.commit();

                    LOGGER.log(Level.INFO, "Usuario registrado correctamente: {0}", user.getLogin());
                    return new Message(MessageType.OK_RESPONSE, user);
                } else {
                    // Si no se pudo insertar en res_users, hacer rollback
                    conn.rollback();
                    LOGGER.log(Level.SEVERE, "Error al insertar en res_users para el usuario: {0}", user.getLogin());
                    return new Message(MessageType.SQL_ERROR, user);
                }
            } else {
                // Si no se pudo insertar en res_partner, hacer rollback
                conn.rollback();
                LOGGER.log(Level.SEVERE, "Error al insertar en res_partner para el usuario: {0}", user.getLogin());
                return new Message(MessageType.SQL_ERROR, user);
            }

        } catch (SQLException event) {
            // Manejar errores generales de SQL
            try {
                if (conn != null) {
                    conn.rollback();  // Hacer rollback en caso de error
                }
            } catch (SQLException sqlEvent) {
                LOGGER.log(Level.SEVERE, "Error al hacer rollback: {0}", sqlEvent.getMessage());
                return new Message(MessageType.BAD_RESPONSE, user);
            }
            LOGGER.log(Level.SEVERE, "Error al insertar usuario, login repetido: {0}", event.getMessage());
            return new Message(MessageType.LOGIN_EXIST_ERROR, user);

        } finally {
            // Liberar recursos en el bloque finally
            try {
                if (rs != null) {
                    rs.close();  // Cerrar ResultSet
                }
                if (stmtInsertUser != null) {
                    stmtInsertUser.close();  // Cerrar PreparedStatement de usuarios
                }
                if (stmtInsertPartner != null) {
                    stmtInsertPartner.close();  // Cerrar PreparedStatement de socios
                }
                if (conn != null) {
                    pool.releaseConnection(conn);  // Liberar la conexión de vuelta al pool
                }
            } catch (SQLException event) {
                LOGGER.log(Level.SEVERE, "Error al liberar recursos: {0}", event.getMessage());
                return new Message(MessageType.BAD_RESPONSE, user);
            }
        }
    }

    /**
     * Método para validar un usuario (inicio de sesión).
     *
     * Este método realiza las siguientes acciones: 1. Obtiene una conexión del
     * pool. 2. Prepara la consulta SQL para validar el login y la contraseña.
     * 3. Retorna un mensaje que indica si el inicio de sesión fue exitoso o no.
     *
     * @param user El objeto User que contiene la información de inicio de
     * sesión.
     * @return Un objeto Message que indica el resultado de la operación.
     */
    @Override
    public Message signIn(User user) {

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            // Obtener una conexión del pool
            conn = pool.getConnection();

            // Verificar si la conexión es válida
            if (conn == null || !conn.isValid(2)) {
                LOGGER.warning("Error: No se pudo obtener una conexión válida.");
                return new Message(MessageType.CONNECTION_ERROR, user);
            }

            // Preparar la consulta SQL para validar el login
            stmt = conn.prepareStatement(sqlSignInVitaminado);
            stmt.setString(1, user.getLogin());
            stmt.setString(2, user.getPass());

            rs = stmt.executeQuery();

            // Si se encuentra un usuario, el inicio de sesión es válido
            if (rs.next()) {
                User newUser = new User();  // Crear un nuevo objeto User
                newUser.setName(rs.getString("name"));  // Rellenar el nombre
                newUser.setActive(rs.getBoolean("active"));  // Rellenar el estado de actividad
                if (!newUser.getActive()) {
                    return new Message(MessageType.NON_ACTIVE, null);  // El usuario no está activo
                } else {
                    return new Message(MessageType.LOGIN_OK, newUser);  // Inicio de sesión exitoso
                }

            } else {
                return new Message(MessageType.SIGNIN_ERROR, user);  // Error en el inicio de sesión
            }
        } catch (SQLException event) {
            return new Message(MessageType.BAD_RESPONSE, user);  // Error de respuesta en caso de excepción
        } finally {
            // Asegurarse de liberar recursos y la conexión
            if (rs != null) {
                try {
                    rs.close();  // Cerrar ResultSet
                } catch (SQLException event) {
                    return new Message(MessageType.BAD_RESPONSE, user);  // Error al cerrar ResultSet
                }
            }
            if (stmt != null) {
                try {
                    stmt.close();  // Cerrar PreparedStatement
                } catch (SQLException event) {
                    return new Message(MessageType.BAD_RESPONSE, user);  // Error al cerrar PreparedStatement
                }
            }
            if (conn != null) {
                pool.releaseConnection(conn);  // Liberar la conexión de vuelta al pool
            }
        }
    }

    @Override
    public Message getUser(User user) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            // Obtener una conexión del pool
            conn = pool.getConnection();

            // Verificar si la conexión es válida
            if (conn == null || !conn.isValid(2)) {
                LOGGER.warning("Error: No se pudo obtener una conexión válida.");
                return new Message(MessageType.CONNECTION_ERROR, user);
            }

            // Preparar la consulta SQL para validar el login
            stmt = conn.prepareStatement(sqlGetUser);
            stmt.setString(1, user.getLogin());
            stmt.setString(2, user.getPass());

            rs = stmt.executeQuery();

            // Si se encuentra un usuario, el inicio de sesión es válido
            if (rs.next()) {
                User newUser = new User();
                newUser.setLogin(rs.getString("login"));// Crear un nuevo objeto User
                newUser.setName(rs.getString("name"));  // Rellenar el nombre
                newUser.setPass(rs.getString("password"));
                newUser.setStreet(rs.getString("street"));
                newUser.setZip(rs.getString("zip"));
                newUser.setCity(rs.getString("city"));
                newUser.setActive(rs.getBoolean("active"));  // Rellenar el estado de actividad
                return new Message(MessageType.GET_OK, newUser);

            } else {
                return new Message(MessageType.GET_FAIL, user);  // Error en el inicio de sesión
            }
        } catch (SQLException event) {
            return new Message(MessageType.BAD_RESPONSE, user);  // Error de respuesta en caso de excepción
        } finally {
            // Asegurarse de liberar recursos y la conexión
            if (rs != null) {
                try {
                    rs.close();  // Cerrar ResultSet
                } catch (SQLException event) {
                    return new Message(MessageType.BAD_RESPONSE, user);  // Error al cerrar ResultSet
                }
            }
            if (stmt != null) {
                try {
                    stmt.close();  // Cerrar PreparedStatement
                } catch (SQLException event) {
                    return new Message(MessageType.BAD_RESPONSE, user);  // Error al cerrar PreparedStatement
                }
            }
            if (conn != null) {
                pool.releaseConnection(conn);  // Liberar la conexión de vuelta al pool
            }
        }
    }
}
