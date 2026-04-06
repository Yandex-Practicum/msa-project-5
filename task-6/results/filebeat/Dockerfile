FROM docker.elastic.co/beats/filebeat:7.17.28

# Кастомный конфиг
COPY filebeat.yml /usr/share/filebeat/filebeat.yml

USER root
# Мапинг вольюмов с логами
RUN mkdir /usr/share/filebeat/dockerlogs
RUN chown -R root /usr/share/filebeat/
RUN chmod -R go-w /usr/share/filebeat/