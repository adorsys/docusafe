package de.adorsys.docusafe.spring.annotation;

import de.adorsys.docusafe.spring.DocusafeSpringConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Created by peter on 16.11.18 12:19.
 */
@Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
@Target(value = {java.lang.annotation.ElementType.TYPE})
@Documented
@Import({DocusafeSpringConfiguration.class})
public @interface UseDocusafeSpringConfiguration {
}
