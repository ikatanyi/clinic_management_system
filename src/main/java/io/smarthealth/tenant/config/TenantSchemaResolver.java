//package io.smarthealth.tenant.config;
//
//import io.smarthealth.tenant.domain.TenantContext;
//import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
//import org.springframework.stereotype.Component;
//
//@Component
//public class TenantSchemaResolver implements CurrentTenantIdentifierResolver {
//
//    private String defaultTenant = "default_schema";
//
//    @Override
//    public String resolveCurrentTenantIdentifier() {
//        String t = TenantContext.getTenantSchema();
//        if (t != null) {
//            return t;
//        } else {
//            return defaultTenant;
//        }
//    }
//
//    @Override
//    public boolean validateExistingCurrentSessions() {
//        return true;
//    }
//
//}
