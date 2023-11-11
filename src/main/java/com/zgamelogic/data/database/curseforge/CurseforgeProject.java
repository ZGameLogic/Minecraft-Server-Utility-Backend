package com.zgamelogic.data.database.curseforge;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity
@Table(name = "CurseforgeProjects")
public class CurseforgeProject {
    @Id
    private long id;
    private String name;
}
