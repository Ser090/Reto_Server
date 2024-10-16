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

public class Dao implements Signable {

    private static final Logger logger = Logger.getLogger(Dao.class.getName());
    private PostgresConnectionPool pool;

    private final String sqlInsertUser = "INSERT INTO res_users(company_id, partner_id, active, login, password, notification_type)VALUES (1, ?, ?, ?, ?, ?) RETURNING id";
    private final String sqlInsertPartner = "INSERT INTO res_partner (company_id, name, display_name, street, zip, city, email)VALUES (1, ?, ?, ?, ?, ?, ?) RETURNING id";
    private final String sqlSignIn = "SELECT * FROM res_users WHERE login = ? AND password = ?";
    private final String sqlSignInVitaminado = "SELECT * FROM res_users u JOIN res_partner p ON u.partner_id = p.id WHERE u.login = ? AND u.password = ?";

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
            // Obtener una conexión del pool
            conn = pool.getConnection();

            // Verificar si la conexión es válida
            if (conn == null || !conn.isValid(2)) {
                logger.warning("Error: No se pudo obtener una conexión válida.");
                return new Message(MessageType.SIGNUP_ERROR, user);
            }

            // Desactivar autocommit para manejar la transacción manualmente
            conn.setAutoCommit(false);

            // Insertar en res_partner y obtener el ID generado
            stmtInsertPartner = conn.prepareStatement(sqlInsertPartner);
            stmtInsertPartner.setString(1, user.getName());      // nombre
            stmtInsertPartner.setString(2, user.getName());      // display_name
            stmtInsertPartner.setString(3, user.getStreet());    // street
            stmtInsertPartner.setString(4, user.getZip());       // zip
            stmtInsertPartner.setString(5, user.getCity());      // city
            stmtInsertPartner.setString(6, user.getLogin());     // email

            // Ejecutar la consulta y obtener el ID del res_partner
            rs = stmtInsertPartner.executeQuery();
            if (rs.next()) {
                int resPartnerId = rs.getInt("id");

                // Insertar en res_users y obtener el ID generado
                stmtInsertUser = conn.prepareStatement(sqlInsertUser);
                stmtInsertUser.setInt(1, resPartnerId);           // partner_id
                stmtInsertUser.setBoolean(2, user.getActive());   // active
                stmtInsertUser.setString(3, user.getLogin());     // login
                stmtInsertUser.setString(4, user.getPass());      // password
                stmtInsertUser.setString(5, "Email");             // notification_type

                rs = stmtInsertUser.executeQuery();
                if (rs.next()) {
                    int resUserId = rs.getInt("id");
                    user.setResUserId(resUserId);  // Asignar el ID generado al usuario

                    // Confirmar la transacción
                    conn.commit();

                    logger.info("Usuario registrado correctamente: " + user.getLogin());
                    return new Message(MessageType.OK_RESPONSE, user);
                } else {
                    conn.rollback();  // Revertir la transacción si no se puede insertar en res_users
                    logger.severe("Error al insertar en res_users para el usuario: " + user.getLogin());
                    return new Message(MessageType.SIGNUP_ERROR, user);
                }
            } else {
                conn.rollback();  // Revertir la transacción si no se puede insertar en res_partner
                logger.severe("Error al insertar en res_partner para el usuario: " + user.getLogin());
                return new Message(MessageType.SIGNUP_ERROR, user);
            }

        } catch (SQLIntegrityConstraintViolationException e) {
            logger.severe("Error al insertar usuario, login repetido: " + e.getMessage());
            return new Message(MessageType.LOGIN_EXIST_ERROR, user);

        } catch (SQLException e) {
            logger.severe("Error en la transacción de registro: " + e.getMessage());
            try {
                if (conn != null) {
                    conn.rollback();  // Revertir la transacción en caso de error
                }
            } catch (SQLException ex) {
                logger.severe("Error al hacer rollback: " + ex.getMessage());
            }
            return new Message(MessageType.SIGNUP_ERROR, user);

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
                    pool.releaseConnection(conn);
                }
            } catch (SQLException e) {
                logger.severe("Error al liberar recursos: " + e.getMessage());
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

            // Preparar la consulta SQL
            stmt = conn.prepareStatement(sqlSignIn);
            stmt.setString(1, user.getLogin());
            stmt.setString(2, user.getPass());

            rs = stmt.executeQuery();

            // Si se encuentra un usuario, el inicio de sesión es válido
            if (rs.next()) {
                return new Message(MessageType.OK_RESPONSE, user);
            } else {
                return new Message(MessageType.SIGNIN_ERROR, user);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return new Message(MessageType.SIGNIN_ERROR, user);
        } finally {
            // Asegurarse de liberar la conexión y cerrar el ResultSet
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (conn != null) {
                pool.releaseConnection(conn);
            }
        }
    }
}
