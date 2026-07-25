package com.vikasyadavnsit.cdc.data;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VpnConfig {
    private boolean enabled;
    @Builder.Default
    private List<String> restrictedApps = new ArrayList<>();
    @Builder.Default
    private List<String> restrictedWebsites = new ArrayList<>();
    @Builder.Default
    private List<String> trafficLogs = new ArrayList<>();
}
