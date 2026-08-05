using Dapper;
using Npgsql;

var connectionString = Environment.GetEnvironmentVariable("EXPORT_DB_CONNECTION")
    ?? throw new InvalidOperationException("Переменная окружения EXPORT_DB_CONNECTION не задана");
var outputPath = Environment.GetEnvironmentVariable("EXPORT_OUTPUT_PATH") ?? "/export/shipments.csv";

Directory.CreateDirectory(Path.GetDirectoryName(outputPath)!);

await using var connection = new NpgsqlConnection(connectionString);
await connection.OpenAsync();

var shipments = await connection.QueryAsync<Shipment>(
    """
    SELECT id, client_id AS ClientId, driver_id AS DriverId, vehicle_id AS VehicleId, status, created_at AS CreatedAt
    FROM shipments
    ORDER BY id
    """);

await using var writer = new StreamWriter(outputPath, append: false);
await writer.WriteLineAsync("id,client_id,driver_id,vehicle_id,status,created_at");

var rowCount = 0;
foreach (var s in shipments)
{
    await writer.WriteLineAsync($"{s.Id},{s.ClientId},{s.DriverId},{s.VehicleId},{s.Status},{s.CreatedAt:O}");
    rowCount++;
}

Console.WriteLine($"[{DateTime.UtcNow:O}] Экспортировано строк: {rowCount} -> {outputPath}");

record Shipment(int Id, int ClientId, int DriverId, int VehicleId, string Status, DateTime CreatedAt);
