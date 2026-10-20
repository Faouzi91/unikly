package com.unikly.store.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import java.util.List;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

class ModuleBoundariesTests {
    private static final List<String> BUSINESS_MODULES = List.of(
            "identity", "catalog", "cart", "orders", "profile", "reviews");
    private static final List<String> INTERNAL_PACKAGES =
            List.of("api", "domain", "persistence", "security", "infrastructure");

    @Test
    void businessModulesUseOtherModulesThroughApplicationContracts() {
        JavaClasses productionClasses = productionClasses();

        for (String source : BUSINESS_MODULES) {
            for (String target : BUSINESS_MODULES) {
                if (source.equals(target)) {
                    continue;
                }

                String[] privatePackages = INTERNAL_PACKAGES.stream()
                        .map(segment -> "com.unikly.store." + target + "." + segment + "..")
                        .toArray(String[]::new);
                ArchRule rule = noClasses()
                        .that().resideInAPackage("com.unikly.store." + source + "..")
                        .should().dependOnClassesThat().resideInAnyPackage(privatePackages)
                        .because(source + " must call " + target + " through its application API");
                rule.check(productionClasses);
            }
        }
    }

    @Test
    void applicationModulesHaveNoDependencyCycles() {
        slices().matching("com.unikly.store.(*)..")
                .should().beFreeOfCycles()
                .check(productionClasses());
    }

    private JavaClasses productionClasses() {
        return new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.unikly.store");
    }
}
