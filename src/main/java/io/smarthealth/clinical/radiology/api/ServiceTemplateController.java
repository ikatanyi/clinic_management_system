///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//package io.smarthealth.clinical.radiology.api;
//
//import io.smarthealth.clinical.radiology.data.RadiologyTestData;
//import io.smarthealth.clinical.radiology.service.RadiologyService;
//import io.smarthealth.infrastructure.common.PaginationUtil;
//import io.smarthealth.infrastructure.exception.APIException;
//import io.smarthealth.infrastructure.utility.PageDetails;
//import io.smarthealth.infrastructure.utility.Pager;
//import io.swagger.annotations.Api;
//import java.util.List;
//import java.util.stream.Collectors;
//import javax.validation.Valid;
//import org.modelmapper.ModelMapper;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.domain.Pageable;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.ResponseBody;
//import org.springframework.web.bind.annotation.RestController;
//
///**
// *
// * @author Kennedy.Imbenzi
// */
//@RestController
//@RequestMapping("/api/")
//@Api(value = "Service-Template-Controller", description = "Setup of Radiology tests")
//public class ServiceTemplateController {
//
//    @Autowired
//    RadiologyService radiologyService;
//
//    @Autowired
//    ModelMapper modelMapper;
//
//    @PostMapping("/template")
//    public @ResponseBody
//    ResponseEntity<?> createTemplate(@RequestBody @Valid final List<RadiologyTestData> radiologyTestData) {
////        List<RadiologyTestData> radiologyTestList = radiologyService.createRadiologyTest(radiologyTestData)
////                .stream()
////                .map((radiology)->{
////                 RadiologyTestData testdata =  radiology.toData();
////            return testdata;
////            }).collect(Collectors.toList());
////        Pager<List<RadiologyTestData>> pagers = new Pager();        
////        pagers.setCode("0");
////        pagers.setMessage("Success");
////        pagers.setContent(radiologyTestList);
////        PageDetails details = new PageDetails();
////        details.setReportName("Radiology Tests");
////        pagers.setPageDetails(details);        
//        return ResponseEntity.status(HttpStatus.OK).body();      
//    }
//
//    @GetMapping("/template/{id}")
//    public ResponseEntity<?> fetchTemplate(@PathVariable("id") final Long id) {
////        RadiologyTestData radiologyTests = radiologyService.getById(id).toData();
////        if (radiologyTests != null) {
////            return ResponseEntity.ok(radiologyTests);
////        } else {
////            throw APIException.notFound("radiology Test Number {0} not found.", id);
////        }
//    }
//
//    @GetMapping("/template")
//    public ResponseEntity<?> fetchAllTemplates(
////       @RequestParam(value = "page", required = false) Integer page1,
////       @RequestParam(value = "pageSize", required = false) Integer size
//    ) {
//
////        Pageable pageable = PaginationUtil.createPage(page1, size);
////        List<RadiologyTestData> testData = radiologyService.findAll(pageable)
////                .stream()
////                .map((radiology)->{
////                 RadiologyTestData testdata =  radiology.toData();
////            return testdata;
////            }).collect(Collectors.toList());
////        Pager page = new Pager();
////        page.setCode("200");
////        page.setContent(testData);
////        page.setMessage("Radiology Tests fetched successfully");
////        PageDetails details = new PageDetails();
////        details.setPage(1);
////        details.setPerPage(25);
////        details.setReportName("Radiology Tests fetched");
//////        details.setTotalElements(Long.parseLong(String.valueOf(pag.getNumberOfElements())));
////        page.setPageDetails(details);
////        return ResponseEntity.ok(page);
//    }
//}