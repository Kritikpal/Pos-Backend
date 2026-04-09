package com.kritik.POS.restaurant.util;

import com.kritik.POS.common.route.FileRoute;
import org.springframework.util.StringUtils;

public final class ProductImageUrlUtil {

    private ProductImageUrlUtil() {
    }

    public static String toClientUrl(String storedUrl) {
        if (!StringUtils.hasText(storedUrl)) {
            return null;
        }

        String normalized = storedUrl.trim();
        if (normalized.startsWith("http://") || normalized.startsWith("https://")) {
            return normalized;
        }
        normalized = normalized.replace('\\', '/').replaceAll("/{2,}", "/");

        if (normalized.startsWith(FileRoute.UPLOADS_BASE_PATH + "/")) {
            return normalized;
        }

        int uploadsIndex = normalized.indexOf("/uploads/");
        if (uploadsIndex >= 0) {
            return normalized.substring(uploadsIndex);
        }

        if (normalized.startsWith("uploads/")) {
            return "/" + normalized;
        }

        if (normalized.startsWith("/")) {
            return normalized;
        }

        return FileRoute.UPLOADS_BASE_PATH + "/" + normalized;
    }
}
