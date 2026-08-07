package com.printscan.cloud;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "printscan.cloud")
public class CloudProperties {
    private String bootstrapOrg = "기본조직";
    private String bootstrapApiKey = "ORG-DEMO-KEY";
}
