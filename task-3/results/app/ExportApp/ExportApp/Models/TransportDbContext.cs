using Microsoft.EntityFrameworkCore;

namespace ExportApp.Models;

public class TransportDbContext : DbContext
{
    public TransportDbContext()
    {
    }

    public TransportDbContext(DbContextOptions<TransportDbContext> options)
        : base(options)
    {
    }

    public DbSet<Shipment> Shipments { get; set; } = null!;
    public DbSet<ShipmentEvent> ShipmentEvents { get; set; } = null!;
    public DbSet<Driver> Drivers { get; set; } = null!;
    public DbSet<Vehicle> Vehicles { get; set; } = null!;
    public DbSet<Client> Clients { get; set; } = null!;

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        base.OnModelCreating(modelBuilder);

        // Индексы
        modelBuilder.Entity<Shipment>(entity =>
        {
            entity.HasIndex(e => e.CreatedAt);
            entity.HasIndex(e => e.Status);
            entity.HasIndex(e => e.ClientId);
        });

        modelBuilder.Entity<ShipmentEvent>(entity =>
        {
            entity.HasIndex(e => e.EventTimestamp);
            entity.HasIndex(e => e.ShipmentId);
        });

        modelBuilder.Entity<Driver>(entity =>
        {
            entity.HasIndex(e => e.DriverCode).IsUnique();
            entity.HasIndex(e => e.Status);
        });

        modelBuilder.Entity<Vehicle>(entity =>
        {
            entity.HasIndex(e => e.VehicleCode).IsUnique();
            entity.HasIndex(e => e.LicensePlate).IsUnique();
        });

        modelBuilder.Entity<Client>(entity =>
        {
            entity.HasIndex(e => e.ClientCode).IsUnique();
            entity.HasIndex(e => e.Status);
        });

        // Отношения
        modelBuilder.Entity<Shipment>()
            .HasOne(s => s.Client)
            .WithMany()
            .HasForeignKey(s => s.ClientId)
            .OnDelete(DeleteBehavior.Restrict);

        modelBuilder.Entity<Shipment>()
            .HasOne(s => s.Driver)
            .WithMany()
            .HasForeignKey(s => s.DriverId)
            .OnDelete(DeleteBehavior.Restrict);

        modelBuilder.Entity<Shipment>()
            .HasOne(s => s.Vehicle)
            .WithMany()
            .HasForeignKey(s => s.VehicleId)
            .OnDelete(DeleteBehavior.Restrict);

        modelBuilder.Entity<ShipmentEvent>()
            .HasOne(se => se.Shipment)
            .WithMany()
            .HasForeignKey(se => se.ShipmentId)
            .OnDelete(DeleteBehavior.Cascade);
    }
}