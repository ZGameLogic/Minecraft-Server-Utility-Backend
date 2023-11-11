package com.zgamelogic.data.services.minecraft;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@ToString
public class MinecraftServerVersion implements Comparable<MinecraftServerVersion> {
    private String version;
    private String url;

    @Override
    public int compareTo(MinecraftServerVersion o) {
        String v1 = version.split("\\.").length != 3 ? version + ".0" : version;
        String v2 = o.version.split("\\.").length != 3 ? o.version + ".0" : o.version;
        for (int i = 0; i < 3; i++) {
            int num1 = Integer.parseInt(v1.split("\\.")[i]);
            int num2 = Integer.parseInt(v2.split("\\.")[i]);
            if(num1 == num2) continue;
            return Integer.compare(num1, num2) * -1;
        }
        return 0;
    }
}
