package dbserver;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import dbserver.PostgresConnectionPool; // Cambiado a PostgresConnectionPool
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.logging.Logger;
import utilidades.Message;
import utilidades.MessageType;
import utilidades.Signable;
import utilidades.User;

/**
 *
 * @author Urko
 */
public class Dao implements Signable {

    // Logger para registrar eventos y errores
    private static final Logger LOGGER = Logger.getLogger(Dao.class.getName());

    // Instancia del pool de conexiones a PostgreSQL
    private PostgresConnectionPool pool;

    // Consultas SQL para insertar y autenticar usuarios
    private final String sqlInsertUser = "INSERT INTO res_users(company_id, partner_id, active, login, password, notification_type)VALUES (1, ?, ?, ?, ?, ?) RETURNING id";
    private final String sqlInsertPartner = "INSERT INTO res_partner (company_id, name, display_name, street, zip, city, email)VALUES (1, ?, ?, ?, ?, ?, ?) RETURNING id";
    private final String sqlSignIn = "SELECT * FROM res_users WHERE login = ? AND password = ?";
    private final String sqlSignInVitaminado = "SELECT p.name, u.active FROM res_users u JOIN res_partner p ON u.partner_id = p.id WHERE u.login = ? AND u.password = ?";

    // Constructor que inicializa el pool de conexiones
    public Dao(PostgresConnectionPool pool) {
        this.pool = pool;
    }

    // Método para registrar un nuevo usuario
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

                    LOGGER.info("Usuario registrado correctamente: " + user.getLogin());
                    return new Message(MessageType.OK_RESPONSE, user);
                } else {
                    // Si no se pudo insertar en res_users, hacer rollback
                    conn.rollback();
                    LOGGER.severe("Error al insertar en res_users para el usuario: " + user.getLogin());
                    return new Message(MessageType.SQL_ERROR, user);
                }
            } else {
                // Si no se pudo insertar en res_partner, hacer rollback
                conn.rollback();
                LOGGER.severe("Error al insertar en res_partner para el usuario: " + user.getLogin());
                return new Message(MessageType.SQL_ERROR, user);
            }

        } catch (SQLIntegrityConstraintViolationException e) {
            // Manejar el error si el login ya existe
            LOGGER.severe("Error al insertar usuario, login repetido: " + e.getMessage());
            return new Message(MessageType.LOGIN_EXIST_ERROR, user);

        } catch (SQLException e) {
            // Manejar errores generales de SQL

            try {
                if (conn != null) {
                    conn.rollback();  // Hacer rollback en caso de error
                }
            } catch (SQLException ex) {
                LOGGER.severe("Error al hacer rollback: " + ex.getMessage());
                return new Message(MessageType.BAD_RESPONSE, user);
            }
            LOGGER.severe("Error en la transacción de registro: " + e.getMessage());
            return new Message(MessageType.BAD_RESPONSE, user);

        } finally {
            // Liberar recursos en el bloque finally
            try {
                if (rs != null) {
                    rs.close();
                }
                if (stmtInsertUser != null) {
                    stmtInsertUser.close();
                }
                if (stmtInsertPartner != null) {
                    stmtInsertPartner.close();
                }
                if (conn != null) {
                    pool.releaseConnection(conn);  // Liberar la conexión de vuelta al pool
                }
            } catch (SQLException e) {
                LOGGER.severe("Error al liberar recursos: " + e.getMessage());
                return new Message(MessageType.BAD_RESPONSE, user);
            }
        }
    }

    // Método para validar un usuario (inicio de sesión)
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
                newUser.setActive(rs.getBoolean("active"));  // Rellenar el nombre
                if (!newUser.getActive()) {
                    return new Message(MessageType.NON_ACTIVE, null);
                } else {
                    return new Message(MessageType.LOGIN_OK, newUser);
                }

            } else {
                return new Message(MessageType.SIGNIN_ERROR, user);
            }
        } catch (SQLException e) {

            return new Message(MessageType.BAD_RESPONSE, user);
        } finally {
            // Asegurarse de liberar recursos y la conexión
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {

                    return new Message(MessageType.BAD_RESPONSE, user);
                }
            }
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {

                    return new Message(MessageType.BAD_RESPONSE, user);
                }
            }
            if (conn != null) {
                pool.releaseConnection(conn);  // Devolver la conexión al pool
            }
        }
    }
}
