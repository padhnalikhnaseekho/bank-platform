package com.bankplatform.reporting.application.port;

/** Uploads a rendered report file and returns the URI it's now reachable at. */
public interface ReportStorage {

    String upload(String key, byte[] content, String contentType);
}
