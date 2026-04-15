package org.sentinel.nmapservice.service;

import com.github.dockerjava.api.async.ResultCallbackTemplate;
import com.github.dockerjava.api.model.Frame;

public class LogToStringCallback extends ResultCallbackTemplate<LogToStringCallback, Frame> {

    private final StringBuilder sb = new StringBuilder();

    @Override
    public void onNext(Frame frame) {
        sb.append(new String(frame.getPayload()));
    }

    public String getLogs() {
        return sb.toString();
    }
}
