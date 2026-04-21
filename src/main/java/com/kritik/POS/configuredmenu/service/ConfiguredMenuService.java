package com.kritik.POS.configuredmenu.service;

import com.kritik.POS.configuredmenu.models.request.ConfiguredMenuTemplateRequest;
import com.kritik.POS.configuredmenu.models.response.ConfiguredMenuItemSearchDto;
import com.kritik.POS.configuredmenu.models.response.ConfiguredMenuTemplateResponse;

import java.util.List;

public interface ConfiguredMenuService {

    ConfiguredMenuTemplateResponse createTemplate(ConfiguredMenuTemplateRequest request);

    ConfiguredMenuTemplateResponse updateTemplate(Long id, ConfiguredMenuTemplateRequest request);

    ConfiguredMenuTemplateResponse getTemplate(Long id);

    List<ConfiguredMenuTemplateResponse> getTemplates(Long chainId, Long restaurantId, Boolean isActive, String search);

    ConfiguredMenuTemplateResponse previewTemplate(Long id);

    boolean deleteTemplate(Long id);

    List<ConfiguredMenuItemSearchDto> searchCandidateMenuItems(Long chainId,
                                                               Long restaurantId,
                                                               String search,
                                                               Boolean recipeBased,
                                                               Integer limit);
}
