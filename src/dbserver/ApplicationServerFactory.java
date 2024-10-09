/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dbserver;

import java.net.Socket;
import server.Worker;

/**
 *
 * @author Sergio
 */
public class ApplicationServerFactory {

    private static ApplicationServerFactory instance;
    private PostgresConnectionPool pool;

    public ApplicationServerFactory() {
        pool = new PostgresConnectionPool(10);
    }

    public static synchronized ApplicationServerFactory getInstance() {
        if (instance == null) {
            instance = new ApplicationServerFactory();
        }
        return instance;
    }

    public Worker crearWorker(Socket clienteSocket) {
        Dao dao = new Dao(pool);
        return new Worker(clienteSocket, dao);
    }

}
