package io.smarthealth.administration.codes.api;

import io.smarthealth.administration.codes.data.CodeData;
import io.smarthealth.administration.codes.service.CodesService;
import io.smarthealth.infrastructure.common.PaginationUtil;
import io.smarthealth.infrastructure.utility.PageDetails;
import io.smarthealth.infrastructure.utility.Pager;
import io.swagger.annotations.Api;
import java.util.List;
import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Kelsas
 */
@Api
@RestController
@Slf4j
@RequestMapping("/api/v1")
public class CodeRestController {

    private final CodesService service;

    public CodeRestController(CodesService codesService) {
        this.service = codesService;
    }

    @PostMapping("/codes")
    public ResponseEntity<?> createCode(@Valid @RequestBody CodeData codeData) {
        CodeData code = service.createCode(codeData);
        return ResponseEntity.status(HttpStatus.CREATED).body(code);
    }

    @GetMapping("/codes/{id}")
    public CodeData getCode(@PathVariable(value = "id") Long id) {
        CodeData code = service.getCode(id);
        return code;
    }

    @GetMapping("/codes")
    public ResponseEntity<?> getAllCodes(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "pageSize", required = false) Integer size) {

        Pageable pageable = PaginationUtil.createPage(page, size);

        Page<CodeData> list = service.getCodes(pageable);
        Pager<List<CodeData>> pagers = new Pager();
        pagers.setCode("0");
        pagers.setMessage("Success");
        pagers.setContent(list.getContent());
        PageDetails details = new PageDetails();
        details.setPage(list.getNumber() + 1);
        details.setPerPage(list.getSize());
        details.setTotalElements(list.getTotalElements());
        details.setTotalPage(list.getTotalPages());
        details.setReportName("Codes");
        pagers.setPageDetails(details);

        return ResponseEntity.ok(pagers);
    }

    @PutMapping("/codes/{id}")
    public CodeData updateCode(@PathVariable(value = "id") Long id, @Valid @RequestBody CodeData codeData) {
        CodeData code = service.updateCode(id, codeData);
        return code;
    }

    @DeleteMapping("/codes/{id}")
    public ResponseEntity<?> deleteCode(@PathVariable(value = "id") Long id) {
        Long ids = service.deleteCode(id);
        return ResponseEntity.accepted().body(ids);
    }

}
