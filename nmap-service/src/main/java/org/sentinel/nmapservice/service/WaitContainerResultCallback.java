package org.sentinel.nmapservice.service;

import com.github.dockerjava.api.async.ResultCallbackTemplate;
import com.github.dockerjava.api.model.WaitResponse;

public class WaitContainerResultCallback extends ResultCallbackTemplate<WaitContainerResultCallback, WaitResponse> {

    private WaitResponse lastResponse;

    @Override
    public void onNext(WaitResponse response) {
        this.lastResponse = response;
    }

    public WaitResponse getLastResponse() {
        return lastResponse;
    }
}
