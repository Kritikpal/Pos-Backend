package com.kritik.POS.restaurant.controller;

import com.kritik.POS.common.model.ApiResponse;
import com.kritik.POS.restaurant.models.response.UserDashboard;
import com.kritik.POS.restaurant.service.RestaurantService;
import com.kritik.POS.swagger.SwaggerTags;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.kritik.POS.restaurant.route.RestaurantRoute.GET_RESTAURANT_DASHBOARD;

@RestController
@Validated
@RequiredArgsConstructor
public class DashboardController {

    private final RestaurantService restaurantService;

    @Tag(name = SwaggerTags.DASHBOARD)
    @GetMapping(GET_RESTAURANT_DASHBOARD)
    public ResponseEntity<ApiResponse<UserDashboard>> userDashboard(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "pageNumber must be at least 1") Integer pageNumber,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "pageSize must be at least 1") Integer pageSize,
            @RequestParam(defaultValue = "") String searchString,
            @RequestParam(required = false) Long categoryId
    ) {
        UserDashboard userDashboard =
                restaurantService.userDashboard(pageNumber - 1, pageSize, searchString, categoryId);

        return ResponseEntity.ok(ApiResponse.SUCCESS(userDashboard));
    }
}
