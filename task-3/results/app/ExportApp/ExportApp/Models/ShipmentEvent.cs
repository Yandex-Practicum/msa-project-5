using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace ExportApp.Models;

[Table("shipment_events")]
public class ShipmentEvent
{
    [Key]
    [Column("id")]
    public int Id { get; set; }

    [Column("shipment_id")]
    public int ShipmentId { get; set; }

    [Column("event_type")]
    [StringLength(50)]
    public string EventType { get; set; } = string.Empty;

    [Column("event_timestamp")]
    public DateTime EventTimestamp { get; set; }

    [Column("location")]
    [StringLength(500)]
    public string? Location { get; set; }

    [Column("description")]
    [StringLength(1000)]
    public string? Description { get; set; }

    [Column("created_by")]
    [StringLength(100)]
    public string? CreatedBy { get; set; }

    [Column("created_at")]
    public DateTime CreatedAt { get; set; }

    // Navigation property
    [ForeignKey("ShipmentId")]
    public virtual Shipment? Shipment { get; set; }
}