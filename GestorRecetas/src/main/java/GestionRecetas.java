import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;

public class GestionRecetas {

    private static final Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        int opcion;
        do {
            System.out.println("\n--- GESTOR DE RECETAS ---");
            System.out.println("1. Mostrar todas las recetas");
            System.out.println("2. Buscar receta por id");
            System.out.println("3. Añadir receta");
            System.out.println("4. Modificar tiempo y raciones");
            System.out.println("5. Eliminar receta");
            System.out.println("6. Añadir receta y actualizar estadísticas (transacción)");
            System.out.println("7. Buscar receta por nombre (procedimiento almacenado)");
            System.out.println("0. Salir");
            System.out.print("Opción: ");
            opcion = leerEntero();
            switch (opcion) {
                case 1 -> mostrarRecetas();
                case 2 -> buscarPorId();
                case 3 -> anadirReceta();
                case 4 -> modificarReceta();
                case 5 -> eliminarReceta();
                case 6 -> anadirRecetaTransaccion();
                case 7 -> buscarPorNombre();
                case 0 -> System.out.println("Hasta luego.");
                default -> System.out.println("Opción no válida.");
            }
        } while (opcion != 0);
    }

    // ---------- Utilidades de entrada ----------
    private static int leerEntero() {
        try {
            return Integer.parseInt(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static String leerTexto(String mensaje) {
        System.out.print(mensaje);
        return sc.nextLine().trim();
    }

    private static int leerEntero(String mensaje) {
        System.out.print(mensaje);
        return leerEntero();
    }

    private static Receta leerReceta() {
        String nombre = leerTexto("Nombre: ");
        String categoria = leerTexto("Categoría: ");
        int tiempo = leerEntero("Tiempo de preparación (min): ");
        String dificultad = leerTexto("Dificultad: ");
        int raciones = leerEntero("Raciones: ");
        return new Receta(nombre, categoria, tiempo, dificultad, raciones);
    }

    private static Receta crearReceta(ResultSet rs) throws SQLException {
        return new Receta(
                rs.getInt("id"),
                rs.getString("nombre"),
                rs.getString("categoria"),
                rs.getInt("tiempo_preparacion"),
                rs.getString("dificultad"),
                rs.getInt("raciones"));
    }

    // ---------- 1. Mostrar todas ----------
    private static void mostrarRecetas() {
        String sql = "SELECT * FROM recetas";
        try (Connection con = ConexionBD.getConexion();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                System.out.println(crearReceta(rs));
            }
        } catch (SQLException e) {
            System.out.println("Error al mostrar las recetas: " + e.getMessage());
        }
    }

    // ---------- 2. Buscar por id ----------
    private static void buscarPorId() {
        int id = leerEntero("Id de la receta: ");
        String sql = "SELECT * FROM recetas WHERE id = ?";
        try (Connection con = ConexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    System.out.println(crearReceta(rs));
                } else {
                    System.out.println("No existe ninguna receta con ese id.");
                }
            }
        } catch (SQLException e) {
            System.out.println("Error al buscar la receta: " + e.getMessage());
        }
    }

    // ---------- 3. Añadir ----------
    private static void anadirReceta() {
        Receta r = leerReceta();
        String sql = "INSERT INTO recetas (nombre, categoria, tiempo_preparacion, dificultad, raciones) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection con = ConexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, r.getNombre());
            ps.setString(2, r.getCategoria());
            ps.setInt(3, r.getTiempoPreparacion());
            ps.setString(4, r.getDificultad());
            ps.setInt(5, r.getRaciones());
            int filas = ps.executeUpdate();
            System.out.println("Recetas añadidas: " + filas);
        } catch (SQLException e) {
            System.out.println("Error al añadir la receta: " + e.getMessage());
        }
    }

    // ---------- 4. Modificar ----------
    private static void modificarReceta() {
        int id = leerEntero("Id de la receta a modificar: ");
        int tiempo = leerEntero("Nuevo tiempo de preparación (min): ");
        int raciones = leerEntero("Nuevas raciones: ");
        String sql = "UPDATE recetas SET tiempo_preparacion = ?, raciones = ? WHERE id = ?";
        try (Connection con = ConexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, tiempo);
            ps.setInt(2, raciones);
            ps.setInt(3, id);
            int filas = ps.executeUpdate();
            System.out.println(filas > 0 ? "Receta modificada." : "No existe ninguna receta con ese id.");
        } catch (SQLException e) {
            System.out.println("Error al modificar la receta: " + e.getMessage());
        }
    }

    // ---------- 5. Eliminar ----------
    private static void eliminarReceta() {
        int id = leerEntero("Id de la receta a eliminar: ");
        String sql = "DELETE FROM recetas WHERE id = ?";
        try (Connection con = ConexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            int filas = ps.executeUpdate();
            System.out.println(filas > 0 ? "Receta eliminada." : "No existe ninguna receta con ese id.");
        } catch (SQLException e) {
            System.out.println("Error al eliminar la receta: " + e.getMessage());
        }
    }

    // ---------- 6. Transacción ----------
    private static void anadirRecetaTransaccion() {
        Receta r = leerReceta();
        String sqlInsert = "INSERT INTO recetas (nombre, categoria, tiempo_preparacion, dificultad, raciones) "
                + "VALUES (?, ?, ?, ?, ?)";
        String sqlUpdate = "UPDATE estadisticas SET total_recetas = total_recetas + 1 WHERE id = 1";

        Connection con = null;
        try {
            con = ConexionBD.getConexion();
            con.setAutoCommit(false); // inicio de la transacción

            try (PreparedStatement ps1 = con.prepareStatement(sqlInsert);
                 PreparedStatement ps2 = con.prepareStatement(sqlUpdate)) {
                ps1.setString(1, r.getNombre());
                ps1.setString(2, r.getCategoria());
                ps1.setInt(3, r.getTiempoPreparacion());
                ps1.setString(4, r.getDificultad());
                ps1.setInt(5, r.getRaciones());
                ps1.executeUpdate();
                ps2.executeUpdate();
            }

            con.commit();
            System.out.println("Transacción completada: receta añadida y estadísticas actualizadas.");
        } catch (SQLException e) {
            System.out.println("Error en la transacción: " + e.getMessage());
            if (con != null) {
                try {
                    con.rollback();
                    System.out.println("Se han deshecho los cambios (rollback).");
                } catch (SQLException ex) {
                    System.out.println("Error al hacer rollback: " + ex.getMessage());
                }
            }
        } finally {
            if (con != null) {
                try {
                    con.setAutoCommit(true);
                    con.close();
                } catch (SQLException e) {
                    System.out.println("Error al cerrar la conexión: " + e.getMessage());
                }
            }
        }
    }

    // ---------- 7. Procedimiento almacenado ----------
    private static void buscarPorNombre() {
        String nombre = leerTexto("Nombre de la receta: ");
        String sql = "{CALL buscarReceta(?)}";
        try (Connection con = ConexionBD.getConexion();
             CallableStatement cs = con.prepareCall(sql)) {
            cs.setString(1, nombre);
            try (ResultSet rs = cs.executeQuery()) {
                boolean encontrada = false;
                while (rs.next()) {
                    System.out.println(crearReceta(rs));
                    encontrada = true;
                }
                if (!encontrada) {
                    System.out.println("No existe ninguna receta con ese nombre.");
                }
            }
        } catch (SQLException e) {
            System.out.println("Error al ejecutar el procedimiento: " + e.getMessage());
        }
    }
}
