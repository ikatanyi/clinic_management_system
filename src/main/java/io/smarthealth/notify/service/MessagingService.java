/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.smarthealth.notify.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smarthealth.infrastructure.lang.DateRange;
import io.smarthealth.infrastructure.lang.UnsafeOkHttpClient;
import io.smarthealth.notify.data.TextMessageData;
import io.smarthealth.notify.domain.TextMessage;
import io.smarthealth.notify.domain.TextMessageRepository;
import io.smarthealth.notify.domain.enumeration.ReceiverType;
import io.smarthealth.notify.domain.specification.TextMessageSpecification;
import io.smarthealth.organization.facility.domain.Employee;
import io.smarthealth.organization.facility.service.EmployeeService;
import io.smarthealth.organization.person.patient.domain.Patient;
import io.smarthealth.organization.person.patient.service.PatientService;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author kent
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessagingService {

    private final TextMessageRepository messageRepository;
    private final PatientService patientService;
    private final EmployeeService employeeService;

    @Transactional
    public TextMessage createTextMessage(TextMessageData msgData) {
        TextMessage msg = msgData.map();
        if (msgData.getPhoneNumber() == null) {
            if (msgData.getReceiverType() == ReceiverType.patient) {
                Patient patient = patientService.findPatientOrThrow(msgData.getReceiverId());
                msg.setName(patient.getFullName());
                msg.setPhoneNumber(patient.getPrimaryContact());
            } else {
                Employee employee = employeeService.fetchEmployeeByNumberOrThrow(msgData.getReceiverId());
                msg.setName(employee.getFullName());
                msg.setPhoneNumber(employee.getPrimaryContact());
            }
        }
        String status = sendSMS(msg.getPhoneNumber(), msg.getMessage());
        msg.setStatus(status);
        return messageRepository.save(msg);
    }

    public List<TextMessage> createBatchTextMessage(List<TextMessageData> msgDataList) {
        List<TextMessage>msgList = new ArrayList();
        for (TextMessageData msgData : msgDataList) {
            TextMessage msg = msgData.map();
            if (msgData.getPhoneNumber() == null) {
                if (msgData.getReceiverType() == ReceiverType.patient) {
                    Patient patient = patientService.findPatientOrThrow(msgData.getReceiverId());
                    msg.setName(patient.getFullName());
                    msg.setPhoneNumber(patient.getPrimaryContact());
                } else {
                    Employee employee = employeeService.fetchEmployeeByNumberOrThrow(msgData.getReceiverId());
                    msg.setName(employee.getFullName());
                    msg.setPhoneNumber(employee.getPrimaryContact());
                }
            }
            String status = sendSMS(msg.getPhoneNumber(), msg.getMessage());
            msg.setStatus(status);
            msgList.add(msg);
        }
        return messageRepository.saveAll(msgList);
    }

    public Optional<TextMessage> getTextMessage(Long id) {
        return messageRepository.findById(id);
    }

    public Page<TextMessage> getAllTextMessage(String name, String status, String phoneNumber, ReceiverType type, DateRange range, Pageable pageable) {
        Specification<TextMessage> spec = TextMessageSpecification.createSpecification(name, status, phoneNumber, type, range);
        return messageRepository.findAll(spec, pageable);
    }

    public String sendSMS(String phone, String msg) {
        String status = null;
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            OkHttpClient client = UnsafeOkHttpClient.getUnsafeOkHttpClient();
            String encMessage = URLEncoder.encode(msg, StandardCharsets.UTF_8.toString());
            String encPhone = URLEncoder.encode(phone.trim(), StandardCharsets.UTF_8.toString());
            String url = "https://data.smartapplicationsgroup.com/api/smsgateway/send?to=" + encPhone + "&message=" + encMessage;
            log.info("Sending SMS initiated ... ");
            Request request = new Request.Builder()
                    .url(url)
                    .header("X-Gravitee-Api-Key", "86b45e1a-f22c-44d2-a434-a7c4a49f900a")
                    .build();

            okhttp3.Response response = client.newCall(request).execute();
            JsonNode rootNode = objectMapper.readTree(response.body().string());
            status = rootNode.path("AfricasTalkingResponse").path("SMSMessageData").path("Recipients").path("Recipient").path("status").textValue();

        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(MessagingService.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(MessagingService.class.getName()).log(Level.SEVERE, null, ex);
        }
        return status;
    }

}
