package com.walmart.cms.dal.exception;

import java.util.UUID;

/**
 * Signals an optimistic-concurrency conflict: the scope's latest version no
 * longer matches the version on which the caller based its change.
 */
public class StaleConfigurationVersionException extends RuntimeException {
    private final UUID scopeId;
    private final UUID expectedBaseVersionId;
    private final UUID actualLatestVersionId;

    public StaleConfigurationVersionException(
            UUID scopeId, UUID expectedBaseVersionId, UUID actualLatestVersionId) {
        super("Configuration version conflict for scope " + scopeId
                + ": expected base version " + expectedBaseVersionId
                + " but latest version is " + actualLatestVersionId);
        this.scopeId = scopeId;
        this.expectedBaseVersionId = expectedBaseVersionId;
        this.actualLatestVersionId = actualLatestVersionId;
    }

    public UUID getScopeId() { return scopeId; }
    public UUID getExpectedBaseVersionId() { return expectedBaseVersionId; }
    public UUID getActualLatestVersionId() { return actualLatestVersionId; }
}
