package io.smarthealth.auth.config;
 
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;

/**
 *
 * @author Kelsas
 */
@Configuration
@EnableResourceServer
public class ResourceServerConfig extends ResourceServerConfigurerAdapter {
 
    private static final String RESOURCE_ID = "smarthealth-service";
 
    @Override
    public void configure(ResourceServerSecurityConfigurer resources) {
        resources.resourceId(RESOURCE_ID);
    }

    @Override
    public void configure(HttpSecurity http) throws Exception { 
        http.authorizeRequests()
                .antMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()
                .antMatchers("/api/user/updatePassword*").hasAuthority("CHANGE_PASSWORD_PRIVILEGE")
                .and()
                .antMatcher("/api/**")
                .authorizeRequests()
                .antMatchers(HttpMethod.POST, "/api/users").permitAll() //<1>
                .antMatchers(HttpMethod.GET, "/api/users","/v2/api-docs/**","/swagger-ui.html*").permitAll() //<1>
                .anyRequest().authenticated();
    }
}
