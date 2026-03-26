package com.kritik.POS.invoice.model;

import org.springframework.core.io.Resource;

public record FileDownloadResponse(
        Resource resource,
        String fileName
) {}