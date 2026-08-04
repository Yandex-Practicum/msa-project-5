FROM docker.elastic.co/beats/filebeat:7.17.28

COPY --chmod=go-w filebeat.yml /usr/share/filebeat/filebeat.yml

USER root
RUN mkdir -p /usr/share/filebeat/dockerlogs && chown -R root /usr/share/filebeat/
