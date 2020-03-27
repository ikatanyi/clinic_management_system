/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.smarthealth.fileStorage.domain;

import io.smarthealth.administration.servicepoint.domain.ServicePoint;
import io.smarthealth.fileStorage.data.DocumentData;
import io.smarthealth.fileStorage.domain.enumeration.DocumentType;
import io.smarthealth.fileStorage.domain.enumeration.Status;
import io.smarthealth.infrastructure.domain.Identifiable;
import io.smarthealth.organization.person.patient.domain.Patient;
import java.math.BigDecimal;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ForeignKey;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.Data;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 *
 * @author Kennedy.Imbenzi
 */
@Data
@Entity
@Table(name = "document")
@Inheritance(strategy = InheritanceType.JOINED)
public class Document extends Identifiable{
    private String fileName; 
    @ManyToOne
    @JoinColumn(foreignKey=@ForeignKey(name="fk_document_service_patient_id"))
    private Patient patient;
    @ManyToOne
    @JoinColumn(foreignKey=@ForeignKey(name="fk_document_service_point_id"))
    private ServicePoint servicePoint;
    @Enumerated(EnumType.STRING)
    private DocumentType documentType;
    private String documentNumber;
    @Enumerated(EnumType.STRING)
    private Status status; 
    private String notes;
    private String fileType;
    private Long size;
   
    public DocumentData toData(){
        DocumentData data = new DocumentData();
        data.setDocumentNumber(this.getDocumentNumber());
        data.setDocumentType(this.getDocumentType());
        data.setFileName(this.getFileName());
        data.setFileType(this.getFileType());
        data.setNotes(this.getNotes());
        if(this.getPatient()!=null){
            data.setPatientNumber(this.getPatient().getPatientNumber());
            data.setPatientName(this.getPatient().getFullName());
        }
        if(this.getServicePoint()!=null)
            data.setServicePointId(this.getServicePoint().getId());
        
        if(fileName!=null){
            String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/downloadFile/")
                .path(this.fileName)
                .toUriString();
            data.setFileDownloadUri(fileDownloadUri);
        }
        data.setSize(this.getSize());
        data.setStatus(this.getStatus());
        return data;
    }
}