package com.walmart.cms.clientsdk;

/** Unchecked exception raised when a CMS management API call fails. */
public class CmsClientSdkException extends RuntimeException {
    public CmsClientSdkException(String message) { super(message); }
    public CmsClientSdkException(String message, Throwable cause) { super(message, cause); }
}
