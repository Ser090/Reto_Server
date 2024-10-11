package dbserver;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import dbserver.PostgresConnectionPool; // Cambiado a PostgresConnectionPool
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import utilidades.Message;
import utilidades.MessageType;
import utilidades.Signable;
import utilidades.User;

public class Dao implements Signable {

    private static final Logger logger = Logger.getLogger(Dao.class.getName());
    private PostgresConnectionPool pool;

    private final String sqlInsertUser = "INSERT INTO res_users(company_id, partner_id, active, login, password)VALUES (1, ?, ?, ?, ?) RETURNING id";
    private final String sqlInsertPartner = "INSERT INTO res_partner (company_id, name, display_name, street, zip, city, email)VALUES (1, ?, ?, ?, ?, ?, ?) RETURNING id";
    private final String sqlInsertDatosUsuarios = "INSERT INTO datos_usuarios (res_user_id, nombre, apellido, telefono, localidad, provincia, fecha_nacimiento) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private final String sqlSignIn = "SELECT * FROM res_users WHERE login = ? AND password = ?";
    private final String sqlLoginExist = "SELECT * FROM res_users WHERE login = ?";
    private final String sqlCountries = "SELECT name FROM res_country_state WHERE country_id = (SELECT id FROM res_country WHERE code = 'ES')";

    public Dao(PostgresConnectionPool pool) {
        this.pool = pool;
    }

    // Método para registrar un nuevo usuario
    public Message signUp(User user) {

        Connection conn = null;
        PreparedStatement stmtInsertPartner = null;
        PreparedStatement stmtInsertUser = null;
        PreparedStatement stmtInsertDatosUsuarios = null;
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
            stmtInsertPartner.setString(1, user.getName());
            stmtInsertPartner.setString(2, user.getName());
            stmtInsertPartner.setString(3, user.getStreet());
            stmtInsertPartner.setString(4, user.getZip());
            stmtInsertPartner.setString(5, user.getCity());
            stmtInsertPartner.setString(6, user.getLogin());
            rs = stmtInsertPartner.executeQuery();
            if (rs.next()) {
                int resPartnerId = rs.getInt("id");

                // Insertar en res_users y obtener el ID generado
                stmtInsertUser = conn.prepareStatement(sqlInsertPartner);
                stmtInsertUser.setInt(1, resPartnerId);
                stmtInsertUser.setBoolean(2, user.getActive());
                stmtInsertUser.setString(3, user.getLogin());
                stmtInsertUser.setString(4, user.getPass());
                rs = stmtInsertUser.executeQuery();
                if (rs.next()) {
                    int resUserId = rs.getInt("id");
                    user.setResUserId(resUserId);

                    conn.commit();

                } else {
                    conn.rollback();
                    logger.severe("Error al insertar en base de datos: " + user.getLogin());
                    return new Message(MessageType.SIGNUP_ERROR, user);
                }
            } else {
                conn.rollback();
                logger.severe("Error al insertar en base de datos: " + user.getLogin());
                return new Message(MessageType.SIGNUP_ERROR, user);
            }

        } catch (SQLException e) {

            try {
                if (conn != null) {
                    conn.rollback();  // Revertir la transacción en caso de error
                    return new Message(MessageType.SIGNUP_ERROR, user);
                }
            } catch (SQLException ex) {
                logger.severe("Error al insertar en base de datos: " + user.getLogin());
                return new Message(MessageType.SIGNUP_ERROR, user);
            }

        } finally {
            // Asegurarse de liberar la conexión y cerrar recursos
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

                //Todo a ido bien sale por aqui.
                logger.info("Usuario registrado correctamente: " + user.getLogin());
                return new Message(MessageType.OK_RESPONSE, user);

            } catch (SQLException e) {
                logger.severe("Error al insertar en base de datos: " + user.getLogin());
                return new Message(MessageType.SIGNUP_ERROR, user);
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

    // Método para verificar si un usuario ya existe en la base de datos
    public Message loginExist(String login) {

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            // Obtener una conexión del pool
            conn = pool.getConnection();

            // Preparar la consulta SQL
            stmt = conn.prepareStatement(sqlLoginExist);
            stmt.setString(1, login);

            rs = stmt.executeQuery();

            // Si se encuentra un usuario, el inicio de sesión es válido
            if (rs.next()) {
                return new Message(MessageType.LOGIN_OK, login);
            } else {
                return new Message(MessageType.LOGIN_EXIST_ERROR, login);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return new Message(MessageType.SQL_ERROR, login);
        } finally {
            // Asegurarse de liberar la conexión y cerrar el ResultSet
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                    return new Message(MessageType.SQL_ERROR, login);
                }
            }
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                    return new Message(MessageType.SQL_ERROR, login);
                }
            }
            if (conn != null) {
                pool.releaseConnection(conn);
            }
        }
    }

    // Método para obtener las provincias de España
    @Override
    public Message getCountries() {

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<String> provincias = new ArrayList<>();

        try {
            // Obtener una conexión del pool
            conn = pool.getConnection();

            // Preparar la consulta SQL
            stmt = conn.prepareStatement(sqlCountries);
            rs = stmt.executeQuery();

            // Agregar los resultados a la lista
            while (rs.next()) {
                provincias.add(rs.getString("name"));
            }

            return new Message(MessageType.COUNTRIES_OK, provincias);

        } catch (SQLException e) {
            return new Message(MessageType.COUNTRIES_ERROR, e);
        } finally {
            // Asegurarse de liberar la conexión y cerrar ResultSet
            try {
                if (rs != null) {
                    rs.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
                if (conn != null) {
                    pool.releaseConnection(conn);
                }
            } catch (SQLException e) {
                return new Message(MessageType.COUNTRIES_ERROR, e);
            }
        }
    }
}
