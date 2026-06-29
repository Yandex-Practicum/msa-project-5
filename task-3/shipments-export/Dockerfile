FROM alpine:3.19

RUN apk add --no-cache postgresql16-client python3

COPY export.py /app/export.py
RUN chmod +x /app/export.py

WORKDIR /app

ENTRYPOINT ["python3", "/app/export.py"]