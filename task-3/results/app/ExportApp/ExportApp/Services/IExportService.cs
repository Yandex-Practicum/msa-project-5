namespace ExportApp.Services;

public interface IExportService
{
    Task<ExportResult> ExportAsync();
}

public class ExportResult
{
    public bool Success { get; set; }
    public int TotalRowsExported { get; set; }
    public TimeSpan Duration { get; set; }
    public List<string> ExportedFiles { get; set; } = new();
    public List<string> Errors { get; set; } = new();
}