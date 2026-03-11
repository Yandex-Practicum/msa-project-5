using ExportApp.Helpers;
using ExportApp.Models;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging;

namespace ExportApp.Services;

public class ExportService : IExportService
{
    private readonly TransportDbContext _dbContext;
    private readonly ILogger<ExportService> _logger;
    private readonly string _outputDirectory;
    private readonly char _delimiter;
    private readonly bool _includeHeader;

    public ExportService(
        TransportDbContext dbContext,
        ILogger<ExportService> logger,
        IConfiguration configuration)
    {
        _dbContext = dbContext;
        _logger = logger;

        _outputDirectory = configuration["ExportSettings:OutputDirectory"] ?? "/tmp/export";
        _delimiter = configuration["ExportSettings:Delimiter"]?.FirstOrDefault() ?? ';';
        _includeHeader = bool.Parse(configuration["ExportSettings:IncludeHeader"] ?? "true");

        Directory.CreateDirectory(_outputDirectory);
    }

    public async Task<ExportResult> ExportAsync()
    {
        var result = new ExportResult();
        var stopwatch = System.Diagnostics.Stopwatch.StartNew();

        try
        {
            _logger.LogInformation("Starting data export process");
            
            _logger.LogInformation("DB_HOST: {DbHost}", Environment.GetEnvironmentVariable("DB_HOST"));
            _logger.LogInformation("DB_PORT: {DbPort}", Environment.GetEnvironmentVariable("DB_PORT"));
            _logger.LogInformation("DB_NAME: {DbName}", Environment.GetEnvironmentVariable("DB_NAME"));

            // Экспорт shipments за вчерашний день
            var yesterday = DateTime.UtcNow.Date.AddDays(-1);
            var shipmentsCount = await ExportShipmentsAsync(yesterday, result);
            result.TotalRowsExported += shipmentsCount;

            // Экспорт shipment_events за вчерашний день
            var eventsCount = await ExportShipmentEventsAsync(yesterday, result);
            result.TotalRowsExported += eventsCount;

            // Экспорт справочников (полные данные)
            var driversCount = await ExportDriversAsync(result);
            result.TotalRowsExported += driversCount;

            var vehiclesCount = await ExportVehiclesAsync(result);
            result.TotalRowsExported += vehiclesCount;

            var clientsCount = await ExportClientsAsync(result);
            result.TotalRowsExported += clientsCount;

            result.Success = true;
            _logger.LogInformation("Export completed successfully. Total rows: {TotalRows}", result.TotalRowsExported);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Export failed with error");
            result.Success = false;
            result.Errors.Add(ex.Message);
        }
        finally
        {
            stopwatch.Stop();
            result.Duration = stopwatch.Elapsed;
        }

        return result;
    }

    private async Task<int> ExportShipmentsAsync(DateTime date, ExportResult result)
    {
        _logger.LogInformation("Exporting shipments for {Date}", date);

        var shipments = await _dbContext.Shipments
            .Where(s => s.CreatedAt.Date == date)
            .OrderBy(s => s.CreatedAt)
            .ToListAsync();

        var fileName = $"shipments_{date:yyyyMMdd}.csv";
        var filePath = Path.Combine(_outputDirectory, fileName);

        CsvHelper.WriteToCsv(filePath, shipments, _delimiter, _includeHeader);

        result.ExportedFiles.Add(filePath);
        _logger.LogInformation("Exported {Count} shipments to {FilePath}", shipments.Count, filePath);

        return shipments.Count;
    }

    private async Task<int> ExportShipmentEventsAsync(DateTime date, ExportResult result)
    {
        _logger.LogInformation("Exporting shipment events for {Date}", date);

        var events = await _dbContext.ShipmentEvents
            .Where(se => se.EventTimestamp.Date == date)
            .OrderBy(se => se.EventTimestamp)
            .ToListAsync();

        var fileName = $"shipment_events_{date:yyyyMMdd}.csv";
        var filePath = Path.Combine(_outputDirectory, fileName);

        CsvHelper.WriteToCsv(filePath, events, _delimiter, _includeHeader);

        result.ExportedFiles.Add(filePath);
        _logger.LogInformation("Exported {Count} shipment events to {FilePath}", events.Count, filePath);

        return events.Count;
    }

    private async Task<int> ExportDriversAsync(ExportResult result)
    {
        _logger.LogInformation("Exporting drivers");

        var drivers = await _dbContext.Drivers
            .OrderBy(d => d.Id)
            .ToListAsync();

        var fileName = "drivers.csv";
        var filePath = Path.Combine(_outputDirectory, fileName);

        CsvHelper.WriteToCsv(filePath, drivers, _delimiter, _includeHeader);

        result.ExportedFiles.Add(filePath);
        _logger.LogInformation("Exported {Count} drivers to {FilePath}", drivers.Count, filePath);

        return drivers.Count;
    }

    private async Task<int> ExportVehiclesAsync(ExportResult result)
    {
        _logger.LogInformation("Exporting vehicles");

        var vehicles = await _dbContext.Vehicles
            .OrderBy(v => v.Id)
            .ToListAsync();

        var fileName = "vehicles.csv";
        var filePath = Path.Combine(_outputDirectory, fileName);

        CsvHelper.WriteToCsv(filePath, vehicles, _delimiter, _includeHeader);

        result.ExportedFiles.Add(filePath);
        _logger.LogInformation("Exported {Count} vehicles to {FilePath}", vehicles.Count, filePath);

        return vehicles.Count;
    }

    private async Task<int> ExportClientsAsync(ExportResult result)
    {
        _logger.LogInformation("Exporting clients");

        var clients = await _dbContext.Clients
            .OrderBy(c => c.Id)
            .ToListAsync();

        var fileName = "clients.csv";
        var filePath = Path.Combine(_outputDirectory, fileName);

        CsvHelper.WriteToCsv(filePath, clients, _delimiter, _includeHeader);

        result.ExportedFiles.Add(filePath);
        _logger.LogInformation("Exported {Count} clients to {FilePath}", clients.Count, filePath);

        return clients.Count;
    }
}