/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.smarthealth.clinical.record.service;

import io.smarthealth.administration.servicepoint.data.ServicePointType;
import io.smarthealth.administration.servicepoint.domain.ServicePoint;
import io.smarthealth.administration.servicepoint.service.ServicePointService;
import io.smarthealth.clinical.queue.domain.PatientQueue;
import io.smarthealth.clinical.queue.service.PatientQueueService;
import io.smarthealth.clinical.record.data.*;
import io.smarthealth.clinical.record.data.DoctorRequestData.RequestType;
import io.smarthealth.clinical.record.data.enums.FullFillerStatusType;
import io.smarthealth.clinical.record.domain.DoctorRequest;
import io.smarthealth.clinical.record.domain.DoctorsRequestRepository;
import io.smarthealth.clinical.record.domain.PrescriptionRepository;
import io.smarthealth.clinical.record.domain.specification.DoctorRequestSpecification;
import io.smarthealth.clinical.visit.domain.Visit;
import io.smarthealth.clinical.visit.domain.enumeration.PaymentMethod;
import io.smarthealth.clinical.visit.service.VisitService;
import io.smarthealth.infrastructure.exception.APIException;
import io.smarthealth.infrastructure.lang.DateConverter;
import io.smarthealth.infrastructure.lang.DateRange;
import io.smarthealth.infrastructure.utility.PageDetails;
import io.smarthealth.infrastructure.utility.Pager;
import io.smarthealth.organization.person.patient.domain.Patient;
import io.smarthealth.organization.person.patient.service.PatientService;
import io.smarthealth.stock.item.domain.Item;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.support.PagedListHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author Kennedy.Imbenzi
 */
@Service
@RequiredArgsConstructor
public class DoctorRequestService implements DateConverter {

    // @Autowired
    private final DoctorsRequestRepository doctorRequestRepository;

    //@Autowired
    private final PatientQueueService patientQueueService;

    // @Autowired
    private final ModelMapper modelMapper;

    private final ServicePointService servicePointService;

    private final PrescriptionRepository prescriptionRepository;

    private final VisitService visitService;

    private final PatientService patientService;

    @Transactional
    public List<DoctorRequest> createRequest(List<DoctorRequest> docRequests) {
        List<DoctorRequest> docReqs = doctorRequestRepository.saveAll(docRequests);
        //Send patient to queue
        for (DoctorRequest docRequest : docReqs) {
            PatientQueue patientQueue = new PatientQueue();
//            Department department = departmentService.findByServicePointTypeAndloggedFacility(docRequest.getRequestType());      
            ServicePoint servicePoint = servicePointService.getServicePointByType(ServicePointType.valueOf(docRequest.getRequestType().name()));
            //check if patient is already queued
            if (patientQueueService.patientIsQueued(servicePoint, docRequest.getPatient())) {
                continue;
            }
            patientQueue.setServicePoint(servicePoint);
            patientQueue.setPatient(docRequest.getPatient());
            patientQueue.setSpecialNotes("");
            patientQueue.setStatus(true);
            patientQueue.setUrgency(PatientQueue.QueueUrgency.Medium);
            patientQueue.setVisit(docRequest.getVisit());
            PatientQueue savedQueue = patientQueueService.createPatientQueue(patientQueue);
        }

        return docReqs;

    }

    public Page<DoctorRequest> findAllRequestsByVisitAndRequestType(final Visit visit, final RequestType requestType, Pageable pageable) {
        Page<DoctorRequest> docReqs = doctorRequestRepository.findByVisitAndRequestType(visit, requestType, pageable);
        return docReqs;
    }

    public List<DoctorRequest> fetchRequestByVisitAndItem(final Visit visit, final Item item) {
        return doctorRequestRepository.findByVisitAndItem(visit, item);
    }

    public Page<DoctorRequest> findAllRequestsByVisit(final Visit visit, Pageable pageable) {
        Page<DoctorRequest> docReqs = doctorRequestRepository.findByVisit(visit, pageable);
        return docReqs;
    }

    public Page<DoctorRequest> findAllRequestsByOrderNoAndRequestType(final String orderNo, String requestType, Pageable pageable) {
        Page<DoctorRequest> docReqs = doctorRequestRepository.findByOrderNumberAndRequestType(orderNo, requestType, pageable);
        return docReqs;
    }

    public Page<DoctorRequest> fetchAllPastDoctorRequests(final String visitNumber, final String patientNumber, final RequestType requestType, final FullFillerStatusType fulfillerStatus, final String groupBy, Pageable pageable) {
        Specification<DoctorRequest> spec = DoctorRequestSpecification.createSpecification(visitNumber, patientNumber, requestType, fulfillerStatus, groupBy, null, null, null, null);

        Page<DoctorRequest> docReqs = doctorRequestRepository.findAll(spec, pageable);
        return docReqs;
    }

    public Page<DoctorRequest> fetchAllDoctorRequests(final String visitNumber, final String patientNumber, final RequestType requestType, final FullFillerStatusType fulfillerStatus, final String groupBy, Pageable pageable, Boolean activeVisit, final String term, DateRange range) {
        Specification<DoctorRequest> spec = DoctorRequestSpecification.createSpecification(visitNumber, patientNumber, requestType, fulfillerStatus, groupBy, activeVisit, term,null, range);

        Page<DoctorRequest> docReqs = doctorRequestRepository.findAll(spec, pageable);
        return docReqs;
    }

    public Page<DoctorRequest> getDoctorOrderRequests(String visitNumber, String patientNumber, RequestType requestType, FullFillerStatusType fulfillerStatus, PaymentMethod paymentMethod, DateRange range, Pageable pageable) {
        Specification<DoctorRequest> spec = DoctorRequestSpecification.createSpecification(visitNumber, patientNumber, requestType, fulfillerStatus, null, null, null, paymentMethod, range);

        Page<DoctorRequest> docReqs = doctorRequestRepository.findAll(spec, pageable);
        return docReqs;
    }
    public Page<VisitOrderDTO> getDoctorOrderSummary(RequestType requestType, DateRange range, Pageable pageable){
        if(requestType!=null && range!=null){
            return doctorRequestRepository.findDoctorCashRequests(range.getStartDateTime(), range.getEndDateTime(),requestType,pageable);
        }else if(requestType!=null && range == null){
            return doctorRequestRepository.findDoctorCashRequests(requestType,pageable);
        }else if(range!=null && requestType == null){
            return doctorRequestRepository.findDoctorCashRequests(range.getStartDateTime(), range.getEndDateTime(),pageable);
        }else {
            return doctorRequestRepository.findDoctorCashRequests(pageable);
        }
    }
//    public Page<DoctorRequest> fetchDoctorRequestLine(final String fulfillerStatus, final RequestType requestType, Pageable pageable) {
//        
//        return doctorRequestRepository.findRequestLine(fulfillerStatus, requestType, pageable);
//    }
//    
    public List<DoctorRequest> fetchServiceRequests(final Patient patient, final FullFillerStatusType fullfillerStatus, final RequestType requestType, final Visit visit) {
        Specification<DoctorRequest> spec = DoctorRequestSpecification.createSpecification(visit.getVisitNumber(), patient.getPatientNumber(), requestType, fullfillerStatus, null, null, null, null, null);
        Pageable wholePage = Pageable.unpaged();
        return doctorRequestRepository.findAll(spec, wholePage).getContent();
    }

//    public List<DoctorRequest> fetchServiceRequestsByPatient(final Patient patient, final FullFillerStatusType fullfillerStatus, final RequestType requestType) {
//        return doctorRequestRepository.findServiceRequestsByPatient(patient, fullfillerStatus, requestType);
//    }
    public List<DoctorRequest> fetchServiceRequestsByVisit(final Visit visit, final FullFillerStatusType fullfillerStatus, final RequestType requestType) {
        return doctorRequestRepository.findServiceRequestsByVisit(visit, fullfillerStatus, requestType);
    }

    public Optional<DoctorRequest> getDocRequestById(Long id) {
        Optional<DoctorRequest> entity = doctorRequestRepository.findById(id);
        return entity;
    }

    public Optional<DoctorRequestData> getDocRequestDataById(Long id) {
        Optional<DoctorRequestData> entity = doctorRequestRepository.findById(id).map(p -> DoctorRequestToData(p));
        return entity;
    }

    public DoctorRequestData UpdateDocRequest(DoctorRequestData requestData) {
        DoctorRequest docReq = convertDoctorRequestData(requestData);
        Optional<DoctorRequest> entity = doctorRequestRepository.findById(docReq.getId());
        if (entity.isPresent()) {
            docReq = doctorRequestRepository.save(entity.get());
        }
        return DoctorRequestToData(docReq);
    }

    public ResponseEntity<?> deleteById(long id) {
        try {
            DoctorRequest req = doctorRequestRepository.findById(id).orElse(null);
            if (req != null) {
                if (req.getFulfillerStatus() != FullFillerStatusType.Fulfilled) {
                    doctorRequestRepository.deleteById(id);
                }
            }

            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    public boolean voidRequest(Long id) {
        try {
//            fulfillDocRequest(id);
            DoctorRequest req = doctorRequestRepository.findById(id).orElse(null);
            if (req != null) {
                if(req.getFulfillerStatus()!= FullFillerStatusType.Fulfilled)
                    req.setVoided(Boolean.TRUE);
                req.setFulfillerStatus(FullFillerStatusType.Fulfilled);
                req.setNotes("Voided for a reason");
                doctorRequestRepository.save(req);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            throw APIException.internalError("Error deleting Patient request with id " + id, e.getMessage());
        }
    }

    public void deleteDocRequest(DoctorRequest request) {
        doctorRequestRepository.delete(request);
    }

    public DoctorRequestData DoctorRequestToData(DoctorRequest docRequest) {
        DoctorRequestData docReqData = modelMapper.map(docRequest, DoctorRequestData.class);
        return docReqData;
    }

    public DoctorRequest convertDoctorRequestData(DoctorRequestData docRequestData) {
        DoctorRequest docReqData = modelMapper.map(docRequestData, DoctorRequest.class);
        return docReqData;
    }

    public DoctorRequestItem toData(DoctorRequest d) {
        DoctorRequestItem requestItem = new DoctorRequestItem();
        if (d.getItem() != null) {
            requestItem.setCode(d.getItem().getItemCode());
            requestItem.setItemId(d.getItem().getId());
            requestItem.setItemName(d.getItem().getItemName());
        }

        requestItem.setRate(d.getItemRate());
        requestItem.setCostRate(d.getItemCostRate());

        requestItem.setRequestItemId(d.getId());
        if (d.getRequestType().equals(DoctorRequestData.RequestType.Pharmacy)) {
            requestItem.setPrescriptionData(PrescriptionData.map(prescriptionRepository.findPresriptionByRequestId(d.getId())));
        }
        requestItem.setOrderNo(d.getOrderNumber());
        requestItem.setOrderDate(d.getOrderDate());
        requestItem.setRequestedByName(d.getRequestedBy().getUsername());
        requestItem.setStatus(d.getFulfillerStatus().name());

        return requestItem;
    }

    @Transactional(readOnly = true)
    public Page<DoctorRequestItem> fetchUnfilledRequests(RequestType requestType) {
        return doctorRequestRepository.findAll(DoctorRequestSpecification.unfullfilledRequests(requestType), PageRequest.of(0, 50)).map(x -> toData(x));
    }

    @Transactional(readOnly = true)
    public Pager<?> getUnfilledDoctorRequests(RequestType requestType) {
        Pageable pageable = PageRequest.of(0, 50);
        Page<DoctorRequest> pageList = doctorRequestRepository.findAll(DoctorRequestSpecification.unfullfilledRequests(requestType), pageable);

        List<WaitingRequestsData> waitingRequests = new ArrayList<>();

        pageList.getContent().stream().map((docReq) -> {

            WaitingRequestsData waitingRequest = new WaitingRequestsData();
            waitingRequest.setPatientData(patientService.convertToPatientData(docReq.getPatient()));
            waitingRequest.setPatientNumber(docReq.getPatient().getPatientNumber());
            waitingRequest.setVisitData(visitService.convertVisitEntityToData(docReq.getVisit()));
            waitingRequest.setVisitNumber(docReq.getVisit().getVisitNumber());
            waitingRequest.setRequestId(docReq.getId());
            //find line items by request_id
            List<DoctorRequest> serviceItems = doctorRequestRepository.findAll(DoctorRequestSpecification.unfullfilledRequests(docReq.getPatient().getPatientNumber(), requestType));
            List<DoctorRequestItem> requestItems = new ArrayList<>();
            serviceItems.forEach((r) -> {
                requestItems.add(toData(r));
            });
            waitingRequest.setItem(requestItems);
            return waitingRequest;
        }).forEachOrdered((waitingRequest) -> {
            waitingRequests.add(waitingRequest);
        });

        PagedListHolder waitingPage = new PagedListHolder(waitingRequests);
        waitingPage.setPageSize(pageable.getPageSize()); // number of items per page
        waitingPage.setPage(pageable.getPageNumber());

        Pager<List<WaitingRequestsData>> pagers = new Pager();
        pagers.setCode("0");
        pagers.setMessage("Success");
        pagers.setContent(waitingPage.getPageList());
        PageDetails details = new PageDetails();
        details.setPage(waitingPage.getPage() + 1);
        details.setPerPage(waitingPage.getPageSize());
        details.setTotalElements(Long.valueOf(waitingRequests.size()));
        details.setTotalPage(waitingPage.getPageCount());
        details.setReportName("Doctor Requests");
        pagers.setPageDetails(details);

        return pagers;
    }

}
