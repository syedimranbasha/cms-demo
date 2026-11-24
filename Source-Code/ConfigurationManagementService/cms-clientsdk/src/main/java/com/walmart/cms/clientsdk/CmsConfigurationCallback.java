package com.walmart.cms.clientsdk;

import com.walmart.cms.clientsdk.model.ConfigurationSnapshot;

@FunctionalInterface
public interface CmsConfigurationCallback {
    void onConfigurationUpdated(ConfigurationSnapshot configuration);
}
