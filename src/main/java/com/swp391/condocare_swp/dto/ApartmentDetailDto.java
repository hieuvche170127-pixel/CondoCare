package com.swp391.condocare_swp.dto;

public record ApartmentDetailDto(
        String id,
        String number,
        Integer floor,
        Float area,

        String buildingId,
        String buildingName,

        String status,
        String rentalStatus,
        String images,
        String description,

        Integer totalResident,
        Integer totalVehicle
) {}
