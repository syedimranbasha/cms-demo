package com.walmart.cms.dal.config;

import com.walmart.cms.dal.entity.OrganizationEntity;
import com.walmart.cms.dal.repository.OrganizationRepository;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootConfiguration
@EnableAutoConfiguration
@EntityScan(basePackageClasses = OrganizationEntity.class)
@EnableJpaRepositories(basePackageClasses = OrganizationRepository.class)
public class CmsDalTestApplication {
}
