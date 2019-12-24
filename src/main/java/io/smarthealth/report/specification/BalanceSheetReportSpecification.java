package io.smarthealth.report.specification;

import io.smarthealth.accounting.acc.validation.DateConverter;
import io.smarthealth.infrastructure.common.SecurityUtils;
import io.smarthealth.report.domain.*;
import io.smarthealth.report.spi.DisplayableFieldBuilder;
import io.smarthealth.report.spi.Report;
import io.smarthealth.report.spi.ReportSpecification;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Report(category = "Accounting", identifier = "Balancesheet")
public class BalanceSheetReportSpecification implements ReportSpecification {

    private static final String DATE_RANGE = "Date range";
    private static final String TYPE = "Type";
    private static final String IDENTIFIER = "Identifier";
    private static final String NAME = "Name";
    private static final String BALANCE = "Balance";

    private final EntityManager entityManager;

    private final HashMap<String, String> accountingColumnMapping = new HashMap<>();
    private final HashMap<String, String> allColumnMapping = new HashMap<>();

    public BalanceSheetReportSpecification(final EntityManager entityManager) {
        super();
        this.entityManager = entityManager;
        this.initializeMapping();
    }

    @Override
    public ReportDefinition getReportDefinition() {
        final ReportDefinition reportDefinition = new ReportDefinition();
        reportDefinition.setIdentifier("Balancesheet");
        reportDefinition.setName("Balance Sheet");
        reportDefinition.setDescription("Balance Sheet Report");
        reportDefinition.setQueryParameters(this.buildQueryParameters());
        reportDefinition.setDisplayableFields(this.buildDisplayableFields());
        return reportDefinition;
    }

    @Override
    public ReportPage generateReport(ReportRequest reportRequest, int pageIndex, int size) {
        final ReportDefinition reportDefinition = this.getReportDefinition();
        this.log.info("Generating report {0}.", reportDefinition.getIdentifier());

        final ReportPage reportPage = new ReportPage();
        reportPage.setName(reportDefinition.getName());
        reportPage.setDescription(reportDefinition.getDescription());
        reportPage.setHeader(this.createHeader(reportRequest.getDisplayableFields()));

        final Query accountQuery = this.entityManager.createNativeQuery(this.buildAssetQuery(reportRequest, pageIndex, size));
        final List<?> accountResultList = accountQuery.getResultList();
        reportPage.setRows(this.buildRows(reportRequest, accountResultList));

        reportPage.setHasMore(
                !this.entityManager.createNativeQuery(this.buildAssetQuery(reportRequest, pageIndex + 1, size))
                        .getResultList().isEmpty()
        );

        reportPage.setGeneratedBy(SecurityUtils.getCurrentUserLogin().get());
        reportPage.setGeneratedOn(DateConverter.toIsoString(LocalDateTime.now(Clock.systemUTC())));
        return reportPage;
    }

    @Override
    public void validate(ReportRequest reportRequest) throws IllegalArgumentException {
        final ArrayList<String> unknownFields = new ArrayList<>();
        reportRequest.getQueryParameters()
                .forEach(queryParameter -> {
                    System.err.println("1: "+queryParameter.getName());
                    if (!this.allColumnMapping.keySet().contains(queryParameter.getName())) {
                        unknownFields.add(queryParameter.getName());
                    }
                });

        reportRequest.getDisplayableFields()
                .forEach(displayableField -> {
                    System.err.println("2: "+displayableField.getName());
                    if (!this.allColumnMapping.keySet().contains(displayableField.getName())) {
                        unknownFields.add(displayableField.getName());
                    }
                });

        if (!unknownFields.isEmpty()) {
            throw new IllegalArgumentException(
                    "Unspecified fields requested: " + unknownFields.stream().collect(Collectors.joining(", "))
            );
        }
    }

    private void initializeMapping() {
        this.accountingColumnMapping.put(DATE_RANGE, "acc.created_on");
        this.accountingColumnMapping.put(TYPE, "acc.a_type");
        this.accountingColumnMapping.put(IDENTIFIER, "acc.identifier");
        this.accountingColumnMapping.put(NAME, "acc.a_name");
        this.accountingColumnMapping.put(BALANCE, "acc.balance");

        this.allColumnMapping.putAll(accountingColumnMapping);
    }

    private Header createHeader(List<DisplayableField> displayableFields) {
        final Header header = new Header();
        header.setColumnNames(
                displayableFields
                        .stream()
                        .map(DisplayableField::getName)
                        .collect(Collectors.toList())
        );
        return header;
    }

    private List<Row> buildRows(ReportRequest reportRequest, List<?> accountResultList) {
        final ArrayList<Row> rows = new ArrayList<>();

        final Row totalAssetRow = new Row();
        totalAssetRow.setValues(new ArrayList<>());

        final Value subAssetTotal = new Value();

        final BigDecimal[] assetSubTotal = {new BigDecimal("0.000")};

        accountResultList.forEach(result -> {

            final Row row = new Row();
            row.setValues(new ArrayList<>());

            if (result instanceof Object[]) {
                final Object[] resultValues;
                resultValues = (Object[]) result;

                for (int i = 0; i < resultValues.length; i++) {
                    final Value assetValue = new Value();
                    if (resultValues[i] != null) {
                        assetValue.setValues(new String[]{resultValues[i].toString()});
                    } else {
                        assetValue.setValues(new String[]{});
                    }

                    row.getValues().add(assetValue);

                    assetSubTotal[0] = assetSubTotal[0].add((BigDecimal) resultValues[3]);

                }
            } else {
                final Value value = new Value();
                value.setValues(new String[]{result.toString()});
                row.getValues().add(value);
            }

            rows.add(row);
        });

        subAssetTotal.setValues(new String[]{new StringBuilder().append("TOTAL ASSETS ").append(assetSubTotal[0]).toString()});
        totalAssetRow.getValues().add(subAssetTotal);

        rows.add(totalAssetRow);

        final String liabilityQueryString = this.buildLiabilityQuery(reportRequest);
        final Query liabilityQuery = this.entityManager.createNativeQuery(liabilityQueryString);
        final List<?> liabilityResultList = liabilityQuery.getResultList();

        final Row totalLiabilityRow = new Row();
        totalLiabilityRow.setValues(new ArrayList<>());
        final Value subLiabilityTotal = new Value();

        final BigDecimal[] liabilitySubTotal = {new BigDecimal("0.000")};

        liabilityResultList.forEach(result -> {

            final Row row = new Row();
            row.setValues(new ArrayList<>());

            if (result instanceof Object[]) {
                final Object[] resultValues;
                resultValues = (Object[]) result;

                for (int i = 0; i < resultValues.length; i++) {
                    final Value liabilityValue = new Value();
                    if (resultValues[i] != null) {
                        liabilityValue.setValues(new String[]{resultValues[i].toString()});
                    } else {
                        liabilityValue.setValues(new String[]{});
                    }

                    row.getValues().add(liabilityValue);

                    liabilitySubTotal[0] = liabilitySubTotal[0].add((BigDecimal) resultValues[3]);

                }
            } else {
                final Value value;
                value = new Value();
                value.setValues(new String[]{result.toString()});
                row.getValues().add(value);
            }

            rows.add(row);
        });

        subLiabilityTotal.setValues(new String[]{new StringBuilder().append("TOTAL LIABILITIES ").append(liabilitySubTotal[0]).toString()});
        totalLiabilityRow.getValues().add(subLiabilityTotal);
        rows.add(totalLiabilityRow);

        final String equityQueryString = this.buildEquityQuery(reportRequest);
        final Query equityQuery = this.entityManager.createNativeQuery(equityQueryString);
        final List<?> equityResultList = equityQuery.getResultList();

        final Row totalEquityRow = new Row();
        totalEquityRow.setValues(new ArrayList<>());
        final Value subEquityTotal = new Value();

        final Row totalLiabilityAndEquityRow = new Row();
        totalLiabilityAndEquityRow.setValues(new ArrayList<>());
        final Value totalLiabilityAndEquityValue = new Value();

        final BigDecimal[] equitySubTotal = {new BigDecimal("0.000")};

        equityResultList.forEach(result -> {

            final Row row = new Row();
            row.setValues(new ArrayList<>());

            if (result instanceof Object[]) {
                final Object[] resultValues;
                resultValues = (Object[]) result;

                for (int i = 0; i < resultValues.length; i++) {
                    final Value equityValue = new Value();
                    if (resultValues[i] != null) {
                        equityValue.setValues(new String[]{resultValues[i].toString()});
                    } else {
                        equityValue.setValues(new String[]{});
                    }

                    row.getValues().add(equityValue);

                    equitySubTotal[0] = equitySubTotal[0].add((BigDecimal) resultValues[3]);

                }
            } else {
                final Value value;
                value = new Value();
                value.setValues(new String[]{result.toString()});
                row.getValues().add(value);
            }

            rows.add(row);
        });

        subEquityTotal.setValues(new String[]{new StringBuilder().append("TOTAL EQUITY ").append(equitySubTotal[0]).toString()});
        totalEquityRow.getValues().add(subEquityTotal);
        rows.add(totalEquityRow);

        final BigDecimal liabilityAndEquity = liabilitySubTotal[0].add(equitySubTotal[0]);
        totalLiabilityAndEquityValue.setValues(new String[]{new StringBuilder().append("TOTAL LIABILITIES and EQUITY ").append(liabilityAndEquity).toString()});
        totalLiabilityAndEquityRow.getValues().add(totalLiabilityAndEquityValue);
        rows.add(totalLiabilityAndEquityRow);

        return rows;
    }

    private String buildAssetQuery(final ReportRequest reportRequest, int pageIndex, int size) {
        final StringBuilder query = new StringBuilder("SELECT ");

        final List<DisplayableField> displayableFields = reportRequest.getDisplayableFields();
        final ArrayList<String> columns = new ArrayList<>();
        displayableFields.forEach(displayableField -> {
            final String column = this.accountingColumnMapping.get(displayableField.getName());
            if (column != null) {
                columns.add(column);
            }
        });

        query.append(columns.stream().collect(Collectors.joining(", ")))
                .append(" FROM ")
                .append("acc_accounts acc ")
                .append("WHERE acc.a_type = 'ASSET' ");

        query.append(" ORDER BY acc.identifier");

        return query.toString();
    }

    private String buildLiabilityQuery(final ReportRequest reportRequest) {
        final StringBuilder query = new StringBuilder("SELECT ");

        final List<DisplayableField> displayableFields = reportRequest.getDisplayableFields();
        final ArrayList<String> columns = new ArrayList<>();
        displayableFields.forEach(displayableField -> {
            final String column = this.accountingColumnMapping.get(displayableField.getName());
            if (column != null) {
                columns.add(column);
            }
        });

        query.append(columns.stream().collect(Collectors.joining(", ")))
                .append(" FROM ")
                .append("acc_accounts acc ")
                .append("WHERE acc.a_type = 'LIABILITY' ");

        query.append(" ORDER BY acc.identifier");

        return query.toString();
    }

    private String buildEquityQuery(final ReportRequest reportRequest) {
        final StringBuilder query = new StringBuilder("SELECT ");

        final List<DisplayableField> displayableFields = reportRequest.getDisplayableFields();
        final ArrayList<String> columns = new ArrayList<>();
        displayableFields.forEach(displayableField -> {
            final String column = this.accountingColumnMapping.get(displayableField.getName());
            if (column != null) {
                columns.add(column);
            }
        });

        query.append(columns.stream().collect(Collectors.joining(", ")))
                .append(" FROM ")
                .append("acc_accounts acc ")
                .append("WHERE acc.a_type = 'EQUITY' ");

        query.append(" ORDER BY acc.identifier");

        return query.toString();
    }

    private List<DisplayableField> buildDisplayableFields() {
        return Arrays.asList(
                DisplayableFieldBuilder.create(TYPE, Type.TEXT).mandatory().build(),
                DisplayableFieldBuilder.create(IDENTIFIER, Type.TEXT).mandatory().build(),
                DisplayableFieldBuilder.create(NAME, Type.TEXT).mandatory().build(),
                DisplayableFieldBuilder.create(BALANCE, Type.TEXT).mandatory().build()
        );
    }

    private List<QueryParameter> buildQueryParameters() {
        return Arrays.asList();
    }
}
