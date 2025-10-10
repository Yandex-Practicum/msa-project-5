import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.sql.DriverManager
import java.util.*

fun main() {
    DriverManager.getConnection("jdbc:postgresql://postgres-service:5432/postgres", "postgres", "1221").use { connection ->
        connection.createStatement().use { statement ->
            val resultSet = statement.executeQuery("select id, name, login, additional_data from user_data")
            val fileName = "${UUID.randomUUID()}.csv"
            val writer = BufferedWriter(FileWriter(File("/data/$fileName")))
            writer.write(""""id","name","login","additional_data"""")
            writer.newLine()
            while (resultSet.next()) {
                writer.write("${resultSet.getInt("id")},${resultSet.getString("name")}," +
                        "${resultSet.getString("login")},${resultSet.getString("additional_data")}")
                writer.newLine()
            }
            writer.flush()
            println("FILENAME: $fileName")
        }
    }
}