FROM docker.elastic.co/beats/filebeat:7.17.28

COPY filebeat.yml /usr/share/filebeat/filebeat.yml

USER root
RUN mkdir -p /usr/share/filebeat/input
RUN chown -R root /usr/share/filebeat/
RUN chmod -R go-w /usr/share/filebeat/
