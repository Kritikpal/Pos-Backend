package com.kritik.POS.mobile.controller;

import com.kritik.POS.mobile.dto.request.PosBootstrapRequest;
import com.kritik.POS.mobile.dto.request.PosPullRequest;
import com.kritik.POS.mobile.dto.response.PosSyncResponse;
import com.kritik.POS.mobile.service.PosSyncService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pos/sync")
@RequiredArgsConstructor
public class PosSyncController {

    private final PosSyncService posSyncService;

    @PostMapping("/bootstrap")
    public ResponseEntity<PosSyncResponse> bootstrap(
            @Valid @RequestBody PosBootstrapRequest request
    ) {
        PosSyncResponse response = posSyncService.bootstrap(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/pull")
    public ResponseEntity<PosSyncResponse> pull(
            @Valid @RequestBody PosPullRequest request
    ) {
        PosSyncResponse response = posSyncService.pull(request);
        return ResponseEntity.ok(response);
    }
}