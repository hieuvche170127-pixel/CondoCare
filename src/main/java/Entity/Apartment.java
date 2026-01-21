package Entity;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "apartment", schema = "swp391")
public class Apartment {
    @Id
    @Column(name = "ID", nullable = false, length = 4)
    private String id;

    @Column(name = "number", nullable = false, length = 4)
    private String number;

    @Column(name = "floor", nullable = false)
    private Integer floor;

    @Column(name = "area", nullable = false)
    private Float area;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "building_id", nullable = false)
    private Building building;

    @ColumnDefault("'EMPTY'")
    @Lob
    @Column(name = "status")
    private String status;

    @ColumnDefault("'AVAILABLE'")
    @Lob
    @Column(name = "rental_status")
    private String rentalStatus;

    @Column(name = "images")
    private String images;

    @Column(name = "description")
    private String description;

    @ColumnDefault("0")
    @Column(name = "total_resident")
    private Integer totalResident;

    @ColumnDefault("0")
    @Column(name = "total_vehicle")
    private Integer totalVehicle;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public Integer getFloor() {
        return floor;
    }

    public void setFloor(Integer floor) {
        this.floor = floor;
    }

    public Float getArea() {
        return area;
    }

    public void setArea(Float area) {
        this.area = area;
    }

    public Building getBuilding() {
        return building;
    }

    public void setBuilding(Building building) {
        this.building = building;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRentalStatus() {
        return rentalStatus;
    }

    public void setRentalStatus(String rentalStatus) {
        this.rentalStatus = rentalStatus;
    }

    public String getImages() {
        return images;
    }

    public void setImages(String images) {
        this.images = images;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getTotalResident() {
        return totalResident;
    }

    public void setTotalResident(Integer totalResident) {
        this.totalResident = totalResident;
    }

    public Integer getTotalVehicle() {
        return totalVehicle;
    }

    public void setTotalVehicle(Integer totalVehicle) {
        this.totalVehicle = totalVehicle;
    }

}