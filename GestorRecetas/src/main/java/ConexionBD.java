

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexionBD {
    private static final String URL =
            "jdbc:mysql://localhost:3306/recetario?useSSL=false&serverTimezone=Europe/Madrid";
    private static final String USUARIO = "root";
    private static final String CLAVE = "root"; // cambiar por la tuya

    public static Connection getConexion() throws SQLException {
        return DriverManager.getConnection(URL, USUARIO, CLAVE);
    }
}
