package com.walmart.cms.dal.entity;

public final class TestEntities {
    private TestEntities() {}

    public static ConfigurationVersionEntity version() { return new ConfigurationVersionEntity(); }
    public static ConfigurationDeploymentEntity deployment() { return new ConfigurationDeploymentEntity(); }
    public static DeploymentStageEntity stage() { return new DeploymentStageEntity(); }
    public static DeploymentInstanceEntity instance() { return new DeploymentInstanceEntity(); }
}