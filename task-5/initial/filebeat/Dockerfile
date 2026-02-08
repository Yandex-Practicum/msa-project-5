FROM docker.elastic.co/beats/filebeat:7.17.28

# Copy our custom configuration file
COPY filebeat.yml /usr/share/filebeat/filebeat.yml

USER root
# Create a directory to map volume with all docker log files
RUN chown root:root /usr/share/filebeat/filebeat.yml && \
    chmod 644 /usr/share/filebeat/filebeat.yml
RUN mkdir /usr/share/filebeat/dockerlogs
RUN chown -R root /usr/share/filebeat/
RUN chmod -R go-w /usr/share/filebeat/