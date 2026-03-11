using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace ExportApp.Models;

[Table("shipments")]
public class Shipment
{
    [Key]
    [Column("id")]
    public int Id { get; set; }

    [Column("shipment_number")]
    [StringLength(50)]
    public string ShipmentNumber { get; set; } = string.Empty;

    [Column("client_id")]
    public int ClientId { get; set; }

    [Column("driver_id")]
    public int DriverId { get; set; }

    [Column("vehicle_id")]
    public int VehicleId { get; set; }

    [Column("origin_address")]
    [StringLength(500)]
    public string OriginAddress { get; set; } = string.Empty;

    [Column("destination_address")]
    [StringLength(500)]
    public string DestinationAddress { get; set; } = string.Empty;

    [Column("scheduled_departure")]
    public DateTime ScheduledDeparture { get; set; }

    [Column("scheduled_arrival")]
    public DateTime ScheduledArrival { get; set; }

    [Column("actual_departure")]
    public DateTime? ActualDeparture { get; set; }

    [Column("actual_arrival")]
    public DateTime? ActualArrival { get; set; }

    [Column("status")]
    [StringLength(50)]
    public string Status { get; set; } = string.Empty;

    [Column("weight_kg")]
    public decimal? WeightKg { get; set; }

    [Column("volume_m3")]
    public decimal? VolumeM3 { get; set; }

    [Column("created_at")]
    public DateTime CreatedAt { get; set; }

    [Column("updated_at")]
    public DateTime? UpdatedAt { get; set; }

    // Navigation properties
    [ForeignKey("ClientId")]
    public virtual Client? Client { get; set; }

    [ForeignKey("DriverId")]
    public virtual Driver? Driver { get; set; }

    [ForeignKey("VehicleId")]
    public virtual Vehicle? Vehicle { get; set; }
}