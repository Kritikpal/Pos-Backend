package com.kritik.POS.mobile.repository.row;

import java.time.LocalDateTime;

public interface SyncStreamRow {

    LocalDateTime syncTs();

    String cursorKey();
}
