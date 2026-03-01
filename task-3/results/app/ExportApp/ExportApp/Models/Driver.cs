using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace ExportApp.Models;

[Table("drivers")]
public class Driver
{
    [Key]
    [Column("id")]
    public int Id { get; set; }

    [Column("driver_code")]
    [StringLength(50)]
    public string DriverCode { get; set; } = string.Empty;

    [Column("first_name")]
    [StringLength(100)]
    public string FirstName { get; set; } = string.Empty;

    [Column("last_name")]
    [StringLength(100)]
    public string LastName { get; set; } = string.Empty;

    [Column("phone")]
    [StringLength(50)]
    public string? Phone { get; set; }

    [Column("email")]
    [StringLength(200)]
    public string? Email { get; set; }

    [Column("license_number")]
    [StringLength(50)]
    public string? LicenseNumber { get; set; }

    [Column("license_expiry")]
    public DateTime? LicenseExpiry { get; set; }

    [Column("status")]
    [StringLength(50)]
    public string Status { get; set; } = string.Empty;

    [Column("created_at")]
    public DateTime CreatedAt { get; set; }

    [Column("updated_at")]
    public DateTime? UpdatedAt { get; set; }
}