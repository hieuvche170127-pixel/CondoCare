package com.swp391.condocare_swp.dto;

import lombok.Data;

@Data
public class ApartmentDto {
    private String id;
    private String number;
    private Integer floor;
    private Float area;

    private String buildingId;
    private String buildingName;

    private String status;
    private String rentalStatus;
    private String images;
    private String description;

    private Integer totalResident;
    private Integer totalVehicle;
}