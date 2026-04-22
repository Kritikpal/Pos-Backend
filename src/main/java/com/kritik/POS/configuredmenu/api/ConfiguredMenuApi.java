package com.kritik.POS.configuredmenu.api;

public interface ConfiguredMenuApi {

    ConfiguredMenuTemplateSnapshot getAccessibleActiveTemplate(Long templateId);

    ConfiguredMenuTemplateSnapshot getAccessibleActiveTemplateByParentMenuItemId(Long parentMenuItemId);
}
