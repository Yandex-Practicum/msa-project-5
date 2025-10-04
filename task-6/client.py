import requests
from opentelemetry import trace
from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.sdk.trace.export import BatchSpanProcessor
from opentelemetry.exporter.zipkin.json import ZipkinExporter
from opentelemetry.sdk.resources import SERVICE_NAME, Resource
from opentelemetry.propagate import inject
from opentelemetry.trace.propagation.tracecontext import TraceContextTextMapPropagator


resource = Resource(attributes={SERVICE_NAME: "python-client"})
provider = TracerProvider(resource=resource)
processor = BatchSpanProcessor(ZipkinExporter(endpoint="http://localhost:9411/api/v2/spans"))
provider.add_span_processor(processor)
trace.set_tracer_provider(provider)

tracer = trace.get_tracer(__name__)

def call_api():
    with tracer.start_as_current_span("call-batch-job") as span:
        headers = {}
        inject(headers)
        TraceContextTextMapPropagator().inject(headers)
        span.add_event("Sending request")
        response = requests.post("http://localhost:8080/api/jobs/run", headers=headers)
        print(f"Response: {response.text}")
        span.set_attribute("http.status_code", response.status_code)

if __name__ == "__main__":
    call_api()