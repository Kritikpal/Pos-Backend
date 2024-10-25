package com.kritik.POS.restaurant.controller;

import com.kritik.POS.common.model.ApiResponse;
import com.kritik.POS.restaurant.models.response.UserDashboard;
import com.kritik.POS.restaurant.service.RestaurantService;
import com.kritik.POS.swagger.SwaggerTags;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.kritik.POS.restaurant.route.RestaurantRoute.GET_RESTAURANT_DASHBOARD;

@RestController
public class DashboardController {

    private final RestaurantService restaurantService;

    @Autowired
    public DashboardController(RestaurantService restaurantService) {
        this.restaurantService = restaurantService;
    }

    @Tag(name = SwaggerTags.DASHBOARD)
    @RequestMapping(GET_RESTAURANT_DASHBOARD)
    public ResponseEntity<ApiResponse<UserDashboard>> userDashboard(
            @RequestParam(name = "pageNumber", defaultValue = "1", required = false) Integer pageNumber,
            @RequestParam(name = "limit", defaultValue = "10", required = false) Integer pageSize,
            @RequestParam(name = "q", defaultValue = "", required = false) String searchString,
            @RequestParam(name = "categoryName", defaultValue = "0", required = false) Long categoryId
    ) {
        UserDashboard userDashboard = restaurantService.userDashboard(pageNumber - 1, pageSize, searchString, categoryId);
        return ResponseEntity.ok(ApiResponse.SUCCESS(userDashboard));
    }
}
