package com.kritik.POS.mobile.dto.response;

import com.kritik.POS.mobile.dto.request.SyncTimeCursorBundle;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class PosSyncResponse {
    private Instant serverTime;
    private String syncSessionId;
    private SyncChanges changes;
    private SyncDeletions deletions;
    private SyncTimeCursorBundle nextCursors;
    private boolean hasMore;
}
