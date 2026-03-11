using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace ExportApp.Models;

[Table("vehicles")]
public class Vehicle
{
    [Key]
    [Column("id")]
    public int Id { get; set; }

    [Column("vehicle_code")]
    [StringLength(50)]
    public string VehicleCode { get; set; } = string.Empty;

    [Column("license_plate")]
    [StringLength(50)]
    public string LicensePlate { get; set; } = string.Empty;

    [Column("make")]
    [StringLength(100)]
    public string Make { get; set; } = string.Empty;

    [Column("model")]
    [StringLength(100)]
    public string Model { get; set; } = string.Empty;

    [Column("year")]
    public int Year { get; set; }

    [Column("capacity_kg")]
    public decimal? CapacityKg { get; set; }

    [Column("capacity_m3")]
    public decimal? CapacityM3 { get; set; }

    [Column("status")]
    [StringLength(50)]
    public string Status { get; set; } = string.Empty;

    [Column("created_at")]
    public DateTime CreatedAt { get; set; }

    [Column("updated_at")]
    public DateTime? UpdatedAt { get; set; }
}