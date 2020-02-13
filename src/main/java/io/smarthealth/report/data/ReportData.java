/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.smarthealth.report.data;

import io.smarthealth.infrastructure.reports.domain.ExportFormat;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import java.util.Map;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import lombok.Data;

/**
 *
 * @author Kennedy.Imbenzi
 */
@Data
public class ReportData {
    @ApiModelProperty(required = false, hidden = false)
    private String reportName;
    private Map<String, Object> filters;
    @Enumerated(EnumType.STRING)
    private ExportFormat format;
    private List data;
}
