package judge_platform.architecture;

import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.Query;
import org.slf4j.Logger;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.GeneralCodingRules.*;

@AnalyzeClasses(packages = "backend.judge")
public class PlatformArchitectureTest {

    @ArchTest
    static final ArchRule no_native_sql_queries = methods()
            .that().areDeclaredInClassesThat().resideInAPackage("..repository..")
            .and().areAnnotatedWith(Query.class)
            .should(new ArchCondition<>("not be native SQL queries") {
                @Override
                public void check(JavaMethod method, ConditionEvents events) {
                    Query query = method.getAnnotationOfType(Query.class);
                    if (query.nativeQuery()) {
                        String message = String.format("Method [%s] utilizes a native SQL query shortcut.", method.getFullName());
                        events.add(SimpleConditionEvent.violated(method, message));
                    }
                }
            })
            .allowEmptyShould(true) // Allows test to pass while repository layer is empty
            .because("Native SQL strings bypass JPA compile-time verification and break database dialect flexibility.");

    @ArchTest
    static final ArchRule transactional_methods_must_never_publish_to_kafka = methods()
            .that().areAnnotatedWith(Transactional.class)
            .should(new ArchCondition<>("not invoke Kafka components or producers directly") {
                @Override
                public void check(JavaMethod method, ConditionEvents events) {
                    method.getCallsFromSelf().forEach(call -> {
                        String targetPackage = call.getTarget().getOwner().getPackageName();
                        if (targetPackage.contains(".producer") || targetPackage.contains(".kafka")) {
                            String message = String.format("Transactional method [%s] directly calls down to Kafka target [%s].",
                                    method.getFullName(), call.getTarget().getFullName());
                            events.add(SimpleConditionEvent.violated(method, message));
                        }
                    });
                }
            })
            .allowEmptyShould(true)
            .because("To prevent distributed state desync, events must be saved to the outbox table inside the database transaction boundary.");

    @ArchTest
    static final ArchRule events_and_dtos_must_be_records = classes()
            .that().resideInAPackage("..dto..")
            .or().resideInAPackage("..event..")
            .should().beRecords()
            .allowEmptyShould(true)
            .because("Multi-threaded message parsing requires strictly immutable structures to eliminate memory race conditions.");

    @ArchTest
    static final ArchRule controllers_must_never_access_repositories_directly = noClasses()
            .that().resideInAPackage("..controller..")
            .should().dependOnClassesThat().resideInAPackage("..repository..")
            .allowEmptyShould(true)
            .because("Bypassing the service layer circumvents essential security validation and transaction management boundaries.");

    @ArchTest
    static final ArchRule restrict_process_generation_to_sandbox = noClasses()
            .that().resideOutsideOfPackage("..sandbox..")
            .should().dependOnClassesThat().haveFullyQualifiedName(ProcessBuilder.class.getName())
            .orShould().dependOnClassesThat().haveFullyQualifiedName(Runtime.class.getName())
            .allowEmptyShould(true)
            .because("Untrusted submission runtime tracking must be strictly compartmentalized inside the sandboxed infrastructure.");

    @ArchTest
    static final ArchRule enforce_clean_logging_practices = fields()
            .that().haveRawType(Logger.class)
            .should(new ArchCondition<>("be private, static, final, and named 'log'") {
                @Override
                public void check(JavaField field, ConditionEvents events) {
                    boolean isValid = field.getModifiers().contains(com.tngtech.archunit.core.domain.JavaModifier.PRIVATE)
                            && field.getModifiers().contains(com.tngtech.archunit.core.domain.JavaModifier.STATIC)
                            && field.getModifiers().contains(com.tngtech.archunit.core.domain.JavaModifier.FINAL)
                            && field.getName().equals("log");

                    if (!isValid) {
                        String message = String.format("Logger field [%s] in class [%s] must be declared as: private static final Logger log;",
                                field.getName(), field.getOwner().getName());
                        events.add(SimpleConditionEvent.violated(field, message));
                    }
                }
            })
            .allowEmptyShould(true)
            .because("We enforce structural logging uniformity across all platform subsystems.");

    @ArchTest
    static final ArchRule no_standard_output_streams = NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;
}