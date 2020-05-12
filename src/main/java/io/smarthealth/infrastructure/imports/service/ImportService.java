/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.smarthealth.infrastructure.imports.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

/**
 *
 * @author Kennedy.Imbenzi
 */
@Service
public class ImportService {

    public void exportExcel(String name, String fileName, Map<Integer, List> data, HttpServletResponse response) throws IOException {

        //Blank workbook
        XSSFWorkbook workbook = new XSSFWorkbook();

        //Create a blank sheet
        XSSFSheet sheet = workbook.createSheet(name);

        CellStyle cellStyle = workbook.createCellStyle();
        CreationHelper createHelper = workbook.getCreationHelper();
        cellStyle.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-MM-dd"));

        // Create a Font for styling header cells
        XSSFFont headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
//        headerFont.setColor(IndexedColors.RED.getIndex());

        // Create a CellStyle with the font
        CellStyle headerCellStyle = workbook.createCellStyle();
        headerCellStyle.setFont(headerFont);

        ServletOutputStream servletOutputStream = response.getOutputStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        //Iterate over data and write to sheet
        Set<Integer> keyset = data.keySet();
        int rownum = 0;
        for (int key : keyset) {
            Row row = sheet.createRow(rownum++);
            List objArr = data.get(key);
            int cellnum = 0;
            for (Object obj : objArr) {
                Cell cell = row.createCell(cellnum++);

                // Create cells
                cell.setCellValue(String.valueOf(obj));

                cell.setCellStyle(headerCellStyle);
            }
        }
        try {
            //Write the workbook in file system
            response.setContentType("application/vnd.ms-excel");
            response.setHeader("Content-Disposition", "attachment; filename=" + fileName + ".xls");
            workbook.write(response.getOutputStream());
            workbook.close();

            System.out.println();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
