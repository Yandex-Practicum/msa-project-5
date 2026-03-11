using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Serilog;
using ExportApp.Models;
using ExportApp.Services;

namespace ExportApp;

class Program
{
    static async Task<int> Main(string[] args)
    {
        // Настройка конфигурации
        var configuration = new ConfigurationBuilder()
            .SetBasePath(Directory.GetCurrentDirectory())
            .AddJsonFile("appsettings.json", optional: true, reloadOnChange: true)
            .AddJsonFile($"appsettings.{Environment.GetEnvironmentVariable("ASPNETCORE_ENVIRONMENT") ?? "Docker"}.json", optional: true)
            .AddEnvironmentVariables()
            .Build();

        // Настройка логирования Serilog
        var logPath = Environment.GetEnvironmentVariable("LOG_PATH") ?? "/var/log/export_job";
        Directory.CreateDirectory(logPath);

        Log.Logger = new LoggerConfiguration()
            .MinimumLevel.Information()
            .WriteTo.Console(
                outputTemplate: "{Timestamp:yyyy-MM-dd HH:mm:ss} [{Level:u3}] {Message:lj}{NewLine}{Exception}")
            .WriteTo.File(
                path: $"{logPath}/export-.log",
                outputTemplate: "{Timestamp:yyyy-MM-dd HH:mm:ss} [{Level:u3}] {Message:lj}{NewLine}{Exception}")
            .CreateLogger();

        try
        {
            Log.Information("========================================");
            Log.Information("Starting Transport Data Exporter");
            Log.Information($"Timestamp: {DateTime.Now:yyyy-MM-dd HH:mm:ss}");
            Log.Information("========================================");

            // Настройка DI контейнера
            var services = new ServiceCollection();
            services.AddSingleton<IConfiguration>(configuration);
            
            ConfigureServices(services, configuration);
            var serviceProvider = services.BuildServiceProvider();

            // Применение миграций (опционально)
            var applyMigrations = Environment.GetEnvironmentVariable("APPLY_MIGRATIONS") ?? "false";
            if (applyMigrations.ToLower() == "true")
            {
                Log.Information("Applying database migrations...");
                using var scopeMigrations = serviceProvider.CreateScope();
                var dbContext = scopeMigrations.ServiceProvider.GetRequiredService<TransportDbContext>();
                await dbContext.Database.MigrateAsync();
                Log.Information("Database migrations applied successfully");
            }

            // Запуск экспорта
            using var scope = serviceProvider.CreateScope();
            var exportService = scope.ServiceProvider.GetRequiredService<IExportService>();
            var result = await exportService.ExportAsync();

            Log.Information("========================================");
            Log.Information(result.Success 
                ? "Export completed successfully" 
                : "Export completed with errors");
            Log.Information($"Total rows exported: {result.TotalRowsExported}");
            Log.Information($"Duration: {result.Duration.TotalSeconds:F2} seconds");
            Log.Information("========================================");

            return result.Success ? 0 : 1;
        }
        catch (Exception ex)
        {
            Log.Fatal(ex, "Application terminated unexpectedly");
            return 1;
        }
        finally
        {
            await Log.CloseAndFlushAsync();
        }
    }

    private static void ConfigureServices(IServiceCollection services, IConfiguration configuration)
    {
        // Логирование
        services.AddLogging(builder =>
        {
            builder.AddSerilog(dispose: true);
        });

        // DbContext
        services.AddDbContext<TransportDbContext>(options =>
        {
            var connectionString = configuration.GetConnectionString("DefaultConnection");
            options.UseNpgsql(connectionString, npgsqlOptions =>
            {
                npgsqlOptions.CommandTimeout(300);
                npgsqlOptions.EnableRetryOnFailure(
                    maxRetryCount: 3,
                    maxRetryDelay: TimeSpan.FromSeconds(30),
                    errorCodesToAdd: null);
            });
        });

        // Сервисы
        services.AddScoped<IExportService, ExportService>();
    }
}