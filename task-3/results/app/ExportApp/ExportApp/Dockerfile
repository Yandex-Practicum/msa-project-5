# Build stage
FROM mcr.microsoft.com/dotnet/sdk:10.0 AS build
ARG BUILD_CONFIGURATION=Release
WORKDIR /src

# Restore dependencies
COPY ["ExportApp.csproj", "./"]
RUN dotnet restore "ExportApp.csproj"

# Build application
COPY . .
WORKDIR "/src"
RUN dotnet build "./ExportApp.csproj" -c $BUILD_CONFIGURATION -o /app/build

# Publish application
FROM build AS publish
ARG BUILD_CONFIGURATION=Release
RUN dotnet publish "./ExportApp.csproj" -c $BUILD_CONFIGURATION -o /app/publish /p:UseAppHost=false

# Runtime stage
FROM mcr.microsoft.com/dotnet/runtime:10.0 AS final
WORKDIR /app

# Copy published output
COPY --from=publish /app/publish .

# Create directories and set permissions for built-in 'app' user
RUN mkdir -p /var/log/export_job /tmp/export && \
    chown -R app:app /var/log/export_job /tmp/export /app

# Use the built-in non-root user
USER app

# Environment variables
ENV DOTNET_ENVIRONMENT=Docker
ENV LOG_PATH=/var/log/export_job
ENV ASPNETCORE_URLS=http://+:8080
ENV DB_HOST=postgres-service.transport-analytics.svc.cluster.local
ENV DB_PORT=5432
ENV DB_NAME=transport_db
ENV DB_USER=postgres
ENV DB_PASSWORD=postgres
ENV APPLY_MIGRATIONS=true

ENTRYPOINT ["dotnet", "ExportApp.dll"]