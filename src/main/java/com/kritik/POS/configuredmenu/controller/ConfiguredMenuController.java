package com.kritik.POS.configuredmenu.controller;

import com.kritik.POS.common.model.ApiResponse;
import com.kritik.POS.configuredmenu.models.request.ConfiguredMenuTemplateRequest;
import com.kritik.POS.configuredmenu.models.response.ConfiguredMenuItemSearchDto;
import com.kritik.POS.configuredmenu.models.response.ConfiguredMenuTemplateResponse;
import com.kritik.POS.configuredmenu.route.ConfiguredMenuRoute;
import com.kritik.POS.configuredmenu.service.ConfiguredMenuService;
import com.kritik.POS.swagger.SwaggerTags;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping(ConfiguredMenuRoute.BASE)
public class ConfiguredMenuController {

    private final ConfiguredMenuService configuredMenuService;

    @Tag(name = SwaggerTags.CONFIGURED_MENU)
    @PostMapping
    public ResponseEntity<ApiResponse<ConfiguredMenuTemplateResponse>> createTemplate(
            @RequestBody @Valid ConfiguredMenuTemplateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.SUCCESS(
                configuredMenuService.createTemplate(request),
                "Configured menu template created successfully"
        ));
    }

    @Tag(name = SwaggerTags.CONFIGURED_MENU)
    @PutMapping(ConfiguredMenuRoute.GET_TEMPLATE)
    public ResponseEntity<ApiResponse<ConfiguredMenuTemplateResponse>> updateTemplate(
            @PathVariable Long id,
            @RequestBody @Valid ConfiguredMenuTemplateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.SUCCESS(
                configuredMenuService.updateTemplate(id, request),
                "Configured menu template updated successfully"
        ));
    }

    @Tag(name = SwaggerTags.CONFIGURED_MENU)
    @GetMapping(ConfiguredMenuRoute.GET_TEMPLATE)
    public ResponseEntity<ApiResponse<ConfiguredMenuTemplateResponse>> getTemplate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.SUCCESS(configuredMenuService.getTemplate(id)));
    }

    @Tag(name = SwaggerTags.CONFIGURED_MENU)
    @GetMapping
    public ResponseEntity<ApiResponse<List<ConfiguredMenuTemplateResponse>>> getTemplates(
            @RequestParam(required = false) Long chainId,
            @RequestParam(required = false) Long restaurantId,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false, defaultValue = "") String search
    ) {
        return ResponseEntity.ok(ApiResponse.SUCCESS(
                configuredMenuService.getTemplates(chainId, restaurantId, isActive, search)
        ));
    }

    @Tag(name = SwaggerTags.CONFIGURED_MENU)
    @GetMapping(ConfiguredMenuRoute.GET_TEMPLATE_PREVIEW)
    public ResponseEntity<ApiResponse<ConfiguredMenuTemplateResponse>> previewTemplate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.SUCCESS(configuredMenuService.previewTemplate(id)));
    }

    @Tag(name = SwaggerTags.CONFIGURED_MENU)
    @DeleteMapping(ConfiguredMenuRoute.DELETE_TEMPLATE)
    public ResponseEntity<ApiResponse<Boolean>> deleteTemplate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.SUCCESS(
                configuredMenuService.deleteTemplate(id),
                "Configured menu template deleted successfully"
        ));
    }

    @Tag(name = SwaggerTags.CONFIGURED_MENU)
    @GetMapping(ConfiguredMenuRoute.SEARCH_MENU_ITEMS)
    public ResponseEntity<ApiResponse<List<ConfiguredMenuItemSearchDto>>> searchMenuItems(
            @RequestParam(required = false) Long chainId,
            @RequestParam(required = false) Long restaurantId,
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(required = false) Boolean recipeBased,
            @RequestParam(required = false, defaultValue = "20") Integer limit
    ) {
        return ResponseEntity.ok(ApiResponse.SUCCESS(
                configuredMenuService.searchCandidateMenuItems(chainId, restaurantId, search, recipeBased, limit)
        ));
    }
}
