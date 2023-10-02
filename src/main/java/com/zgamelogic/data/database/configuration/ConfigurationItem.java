package com.zgamelogic.data.database.configuration;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "configuration_items")
public class ConfigurationItem {
    
    @Id
    private String name;
    private String value;
}
