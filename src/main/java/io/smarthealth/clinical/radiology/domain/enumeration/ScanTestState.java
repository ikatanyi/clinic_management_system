/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.smarthealth.clinical.radiology.domain.enumeration;

/**
 *
 * @author Kennedy.Imbenzi
 */
public enum ScanTestState { 
        Scheduled,
        AwaitingSpecimen,
        Accepted,
        Rejected,
        Completed,
        Cancelled,
        AwaitingReview 
}
