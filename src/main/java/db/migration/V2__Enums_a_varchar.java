package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Convierte las columnas ENUM nativas de MySQL/MariaDB a VARCHAR.
 *
 * <p>Hibernate 6 crea {@code @Enumerated(STRING)} como ENUM nativo en MySQL y
 * {@code ddl-auto=update} nunca altera columnas existentes: añadir un valor nuevo a un
 * enumerado (p. ej. RETARDO en asistencia) hacía fallar los INSERT en instalaciones ya
 * creadas ("Data truncated"). Las entidades ahora declaran VARCHAR y esta migración
 * ajusta las bases existentes.
 *
 * <p>No destructiva: conserva los datos (los valores ya son texto), la nulabilidad y el
 * valor por defecto. Idempotente: en una base nueva o ya convertida no hace nada. En
 * otros motores (H2 de las pruebas) se omite.
 */
public class V2__Enums_a_varchar extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection conexion = context.getConnection();
        String motor = conexion.getMetaData().getDatabaseProductName().toLowerCase();
        if (!motor.contains("mysql") && !motor.contains("mariadb")) {
            return;
        }

        List<String> sentencias = new ArrayList<>();
        try (Statement st = conexion.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT table_name, column_name, is_nullable, column_default "
                             + "FROM information_schema.columns "
                             + "WHERE table_schema = DATABASE() AND data_type = 'enum'")) {
            while (rs.next()) {
                String tabla = rs.getString(1);
                String columna = rs.getString(2);
                boolean nula = "YES".equalsIgnoreCase(rs.getString(3));
                String porDefecto = rs.getString(4);
                StringBuilder sql = new StringBuilder("ALTER TABLE `").append(tabla).append("` MODIFY `")
                        .append(columna).append("` VARCHAR(50)").append(nula ? " NULL" : " NOT NULL");
                if (porDefecto != null && !"NULL".equalsIgnoreCase(porDefecto)) {
                    // MariaDB devuelve el valor ya entrecomillado; MySQL sin comillas
                    String valor = porDefecto.startsWith("'") ? porDefecto : "'" + porDefecto.replace("'", "''") + "'";
                    sql.append(" DEFAULT ").append(valor);
                }
                sentencias.add(sql.toString());
            }
        }
        try (Statement st = conexion.createStatement()) {
            for (String sql : sentencias) {
                st.execute(sql);
            }
        }
    }
}
