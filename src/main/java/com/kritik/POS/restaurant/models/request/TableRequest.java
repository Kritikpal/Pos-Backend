package com.kritik.POS.restaurant.models.request;

import jakarta.validation.constraints.NotEmpty;

public record TableRequest(
        Long tableId,
        @NotEmpty(message = "Number of id is required") Integer noOfSeat,
        @NotEmpty(message = "Table number id is required") Integer tableNumber
) {

}
