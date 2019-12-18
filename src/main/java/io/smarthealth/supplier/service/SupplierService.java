package io.smarthealth.supplier.service;

import io.smarthealth.accounting.pricebook.data.PriceBookData;
import io.smarthealth.accounting.pricebook.domain.PriceBook;
import io.smarthealth.accounting.pricebook.service.PricebookService;
import io.smarthealth.administration.app.data.BankAccountData;
import io.smarthealth.administration.app.domain.Address;
import io.smarthealth.administration.app.domain.Contact;
import io.smarthealth.administration.app.domain.Currency;
import io.smarthealth.accounting.payment.domain.PaymentTerms;
import io.smarthealth.administration.app.service.AdminService;
import io.smarthealth.administration.app.service.CurrencyService;
import io.smarthealth.accounting.payment.service.PaymentTermsService;
import io.smarthealth.infrastructure.exception.APIException;
import io.smarthealth.supplier.data.SupplierData;
import io.smarthealth.supplier.domain.Supplier;
import io.smarthealth.supplier.domain.SupplierMetadata;
import io.smarthealth.supplier.domain.SupplierRepository;
import io.smarthealth.supplier.domain.enumeration.SupplierType;
import io.smarthealth.supplier.domain.specification.SupplierSpecification;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

/**
 *
 * @author Kelsas
 */
@Service
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final PaymentTermsService paymentService;
    private final CurrencyService currencyService;
    private final PricebookService pricebookService;
    private final AdminService adminService;

    public SupplierService(SupplierRepository supplierRepository, PaymentTermsService paymentService, CurrencyService currencyService, PricebookService pricebookService, AdminService adminService) {
        this.supplierRepository = supplierRepository;
        this.paymentService = paymentService;
        this.currencyService = currencyService;
        this.pricebookService = pricebookService;
        this.adminService = adminService;
    }

    public SupplierData createSupplier(SupplierData supplierData) {
        Supplier supplier = new Supplier();
        if (supplierData.getSupplierType() != null) {
            supplier.setSupplierType(supplierData.getSupplierType());
        }

        supplier.setSupplierName(supplierData.getSupplierName());
        supplier.setLegalName(supplierData.getLegalName());
        supplier.setTaxNumber(supplierData.getTaxNumber());
        supplier.setWebsite(supplierData.getWebsite());

        if (supplierData.getCurrencyId() != null) {
            Optional<Currency> c = currencyService.getCurrency(supplierData.getCurrencyId());
            if (c.isPresent()) {
                supplier.setCurrency(c.get());
            }
        }
        if (supplierData.getPricebookId() != null) {
            Optional<PriceBook> book = pricebookService.getPricebook(supplierData.getPricebookId());
            if (book.isPresent()) {
                supplier.setPricelist(book.get());
            }
        }

        if (supplierData.getPaymentTermsId() != null) {
            Optional<PaymentTerms> terms = paymentService.getPaymentTerm(supplierData.getPaymentTermsId());
            if (terms.isPresent()) {
                supplier.setPaymentTerms(terms.get());
            }
        }

        if (supplierData.getBank() != null) {
            supplier.setBankAccount(BankAccountData.map(supplierData.getBank()));
        }

        if (supplierData.getAddresses() != null && supplierData.getAddresses() .getPhone()!=null) {
            Address addresses = adminService.createAddress(supplierData.getAddresses());
            addresses.setType(Address.Type.Office);
            supplier.setAddress(addresses);
        }
        
        if (supplierData.getContact() != null) {
            Contact contact=adminService.createContact(supplierData.getContact());
            supplier.setContact(contact);
        }

        Supplier savedSupplier = supplierRepository.save(supplier);
        return savedSupplier.toData();
    }

    public Supplier findOneWithNoFoundDetection(Long id) {
        Supplier supplier = getSupplierById(id)
                .orElseThrow(() -> APIException.notFound("Supplier with id {0} not found.", id));
        return supplier;
    }

    public Optional<Supplier> getSupplierById(Long id) {
        return supplierRepository.findById(id);
    }

    public Optional<Supplier> getSupplierByName(String companyName, String legalName) {
        return supplierRepository.findBySupplierNameOrLegalName(companyName, legalName);
    }

    public Optional<Supplier> getSupplierByName(String supplierName) {
        return supplierRepository.findBySupplierNameOrLegalName(supplierName, supplierName);
    }

    public Page<Supplier> getSuppliers(String type, boolean includeClosed, String term, Pageable page) {
        SupplierType searchType = null;
        if (type != null && EnumUtils.isValidEnum(SupplierType.class, type)) {
            searchType = SupplierType.valueOf(type);
        }
        Specification<Supplier> spec = SupplierSpecification.createSpecification(searchType, term, includeClosed);
        return supplierRepository.findAll(spec, page);
    }

    public SupplierMetadata getSupplierMetadata() {
        SupplierMetadata metadata = new SupplierMetadata();
        List<Currency> currency = currencyService.getCurrencies();
        List<PaymentTerms> paymentTerms = paymentService.getPaymentTerms();
        List<PriceBookData> pricebooks = pricebookService.getPricebooks();
        metadata.setCurrencies(currency);
        metadata.setPaymentTerms(paymentTerms);
        metadata.setPricelists(pricebooks);

        return metadata;
    }
}
