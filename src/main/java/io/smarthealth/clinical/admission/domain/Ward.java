/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.smarthealth.clinical.admission.domain;

import io.smarthealth.infrastructure.domain.Identifiable;
import io.smarthealth.clinical.admission.data.WardData;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.Data;

/**
 *
 * @author Kelsas
 */
@Entity
@Data
@Table(name = "facility_ward")
public class Ward extends Identifiable {

    @Column(name = "ward_name")
    private String name;
    private String description;

    @OneToMany(mappedBy = "ward")
    @JoinColumn(foreignKey=@ForeignKey(name="fk_facility_ward_room_id"))
    private List<Room> rooms = new ArrayList<>();

    private Boolean isActive=Boolean.TRUE;

    public WardData toData() {
        WardData data = new WardData();
        data.setActive(this.isActive);
        data.setDescription(this.description);
        data.setId(this.getId());
        data.setName(this.name);
        data.setRooms(this.rooms
                .stream().map(x -> x.toData())
                .collect(Collectors.toList())
        );
        return data;
    }
}
