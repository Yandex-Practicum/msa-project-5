using System.Reflection;
using System.Text;

namespace ExportApp.Helpers;

public static class CsvHelper
{
    public static void WriteToCsv<T>(string filePath, IEnumerable<T> data, char delimiter = ';', bool includeHeader = true)
    {
        var items = data.ToList();
        if (!items.Any())
        {
            File.WriteAllText(filePath, string.Empty, Encoding.UTF8);
            return;
        }

        var properties = typeof(T).GetProperties(BindingFlags.Public | BindingFlags.Instance)
            .Where(p => p.CanRead)
            .ToArray();

        var sb = new StringBuilder();

        // Header
        if (includeHeader)
        {
            var header = string.Join(delimiter.ToString(), properties.Select(p => EscapeCsvValue(p.Name, delimiter)));
            sb.AppendLine(header);
        }

        // Data rows
        foreach (var item in items)
        {
            var values = properties.Select(p =>
            {
                var value = p.GetValue(item);
                return EscapeCsvValue(value?.ToString() ?? string.Empty, delimiter);
            });

            sb.AppendLine(string.Join(delimiter.ToString(), values));
        }

        File.WriteAllText(filePath, sb.ToString(), Encoding.UTF8);
    }

    private static string EscapeCsvValue(string value, char delimiter)
    {
        if (string.IsNullOrEmpty(value))
            return string.Empty;

        // Если значение содержит разделитель, кавычки или перенос строки - экранируем
        if (value.Contains(delimiter) || value.Contains('"') || value.Contains('\n') || value.Contains('\r'))
        {
            return $"\"{value.Replace("\"", "\"\"")}\"";
        }

        return value;
    }
}