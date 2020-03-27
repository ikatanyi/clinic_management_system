/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.smarthealth.fileStorage.service;

import io.smarthealth.administration.servicepoint.domain.ServicePoint;
import io.smarthealth.administration.servicepoint.service.ServicePointService;
import io.smarthealth.fileStorage.data.DocumentData;
import io.smarthealth.fileStorage.domain.Document;
import io.smarthealth.fileStorage.domain.FileStorageRepository;
import io.smarthealth.fileStorage.domain.enumeration.DocumentType;
import io.smarthealth.fileStorage.domain.enumeration.Status;
import io.smarthealth.fileStorage.domain.specification.DocumentSpecification;
import io.smarthealth.infrastructure.exception.APIException;
import io.smarthealth.infrastructure.imports.service.UploadService;
import io.smarthealth.infrastructure.lang.DateRange;
import io.smarthealth.organization.person.patient.domain.Patient;
import io.smarthealth.organization.person.patient.service.PatientService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 *
 * @author Kennedy.Imbenzi
 */
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final FileStorageRepository fileStorageRepository;

    private final UploadService uploadService;

    private final PatientService patientService;

    private final ServicePointService servicePointService;

    @Transactional
    public Document documentUpload(DocumentData documentData) {
        Document document = documentData.fromData();
        Patient patient = patientService.findPatientOrThrow(documentData.getPatientNumber());
        ServicePoint servicePoint = servicePointService.getServicePoint(documentData.getServicePointId());
        
        document.setPatient(patient);
        document.setServicePoint(servicePoint);

        String fileName = uploadService.storeFile(documentData.getDocfile());
        document.setFileName(fileName);
        return fileStorageRepository.save(document);
    }

    public List<Document> batchDocumentUpload(List<DocumentData> batchDocumentData) {
        List<Document> documents = new ArrayList();
        batchDocumentData
                .stream()
                .map((documentData) -> {
                    Document document = documentData.fromData();
                    Patient patient = patientService.findPatientOrThrow(documentData.getPatientNumber());
                    ServicePoint servicePoint = servicePointService.getServicePoint(documentData.getServicePointId());
                    document.setPatient(patient);
                    document.setServicePoint(servicePoint);
                    return document;
                }).forEachOrdered((document) -> {
            documents.add(document);
        });
        return fileStorageRepository.saveAll(documents);
    }

    @Transactional
    public Document UpdateDocument(MultipartFile file, Long id, DocumentData documentData) {
        Document document = getDocumentByIdWithFailDetection(id);
        Patient patient = patientService.findPatientOrThrow(documentData.getPatientNumber());
        ServicePoint servicePoint = servicePointService.getServicePoint(documentData.getServicePointId());
        Long size = file.getSize();
        String fileType = file.getContentType();
        document.setSize(size);
        document.setFileType(fileType);
        document.setPatient(patient);
        document.setServicePoint(servicePoint);
        String fileName = uploadService.storeFile(file);
        document.setFileName(fileName);

        return fileStorageRepository.save(document);

    }

    public Document getDocumentByIdWithFailDetection(Long id) {
        return fileStorageRepository.findById(id).orElseThrow(() -> APIException.notFound("Document identified by id {0} not found ", id));
    }

    public Optional<Document> getDocumentById(Long id) {
        return fileStorageRepository.findById(id);
    }

    @Transactional
    public Page<Document> findAllDocuments(String PatientNumber,DocumentType documentType, Status status, Long servicePointId, DateRange range, Pageable pgbl) {
        Specification spec =  DocumentSpecification.createSpecification(PatientNumber, documentType, status, servicePointId, range);
        return fileStorageRepository.findAll(spec, pgbl);
    }
}