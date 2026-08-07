import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

public final class BatchJobClient {

    private static final String DEFAULT_ENDPOINT =
            "http://localhost:18080/api/v1/jobs/import-products";

    private BatchJobClient() {
    }

    public static void main(String[] args) throws Exception {
        URI endpoint = URI.create(args.length == 0 ? DEFAULT_ENDPOINT : args[0]);
        String traceId = UUID.randomUUID().toString().replace("-", "");
        String parentSpanId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String traceparent = "00-" + traceId + "-" + parentSpanId + "-01";

        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .header("traceparent", traceparent)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("traceId=" + traceId);
        System.out.println("HTTP " + response.statusCode());
        System.out.println(response.body());

        if (response.statusCode() / 100 != 2) {
            throw new IllegalStateException("Batch API returned HTTP " + response.statusCode());
        }
    }
}
