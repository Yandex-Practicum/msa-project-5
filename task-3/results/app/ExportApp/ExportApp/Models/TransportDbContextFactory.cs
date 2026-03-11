using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Design;
using Microsoft.Extensions.Configuration;

namespace ExportApp.Models;

public class TransportDbContextFactory : IDesignTimeDbContextFactory<TransportDbContext>
{
    public TransportDbContext CreateDbContext(string[] args)
    {
        // Чтение конфигурации из appsettings.json
        var configuration = new ConfigurationBuilder()
            .SetBasePath(Directory.GetCurrentDirectory())
            .AddJsonFile("appsettings.json", optional: true)
            .AddEnvironmentVariables()
            .Build();

        var connectionString = configuration.GetConnectionString("DefaultConnection")
                               ?? "Host=localhost;Port=5432;Database=transport_db;Username=postgres;Password=postgres";

        var builder = new DbContextOptionsBuilder<TransportDbContext>();
        builder.UseNpgsql(connectionString, options =>
        {
            options.CommandTimeout(300);
        });

        return new TransportDbContext(builder.Options);
    }
}