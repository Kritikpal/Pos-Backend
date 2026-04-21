package com.kritik.POS.mobile.service;


import com.kritik.POS.mobile.dto.request.PosBootstrapRequest;
import com.kritik.POS.mobile.dto.request.PosPullRequest;
import com.kritik.POS.mobile.dto.response.PosSyncResponse;

public interface PosSyncService {

    PosSyncResponse bootstrap(PosBootstrapRequest request);

    PosSyncResponse pull(PosPullRequest request);
}