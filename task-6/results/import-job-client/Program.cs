using System.Diagnostics;
using OpenTelemetry;
using OpenTelemetry.Trace;

var jobEndpoint = Environment.GetEnvironmentVariable("JOB_ENDPOINT")
    ?? "http://localhost:8080/jobs/import-product";

var activitySource = new ActivitySource("ImportJobClient");

// AddSource настраивает провайдера трейсов.
// создаётся дочерний спан на сам HTTP-вызов с тегами http.method/url/status.
using var tracerProvider = Sdk.CreateTracerProviderBuilder()
    .AddSource("ImportJobClient")
    .AddHttpClientInstrumentation()
    .AddConsoleExporter()
    .Build();

using var httpClient = new HttpClient();

using var activity = activitySource.StartActivity("trigger-import-job", ActivityKind.Client);

Console.WriteLine(
    $"[{DateTime.UtcNow:O}] traceId={activity?.TraceId} spanId={activity?.SpanId} uri={jobEndpoint} -> отправка запроса");

try
{
    var response = await httpClient.PostAsync(jobEndpoint, content: null);
    var body = await response.Content.ReadAsStringAsync();

    Console.WriteLine(
        $"[{DateTime.UtcNow:O}] traceId={activity?.TraceId} spanId={activity?.SpanId} uri={jobEndpoint} " +
        $"status={(int)response.StatusCode} body=\"{body}\" -> ответ получен");
}
catch (HttpRequestException ex)
{
    activity?.SetStatus(ActivityStatusCode.Error, ex.Message);
    Console.WriteLine(
        $"[{DateTime.UtcNow:O}] traceId={activity?.TraceId} spanId={activity?.SpanId} uri={jobEndpoint} " +
        $"error=\"{ex.Message}\" -> запрос не выполнен");
    throw;
}
