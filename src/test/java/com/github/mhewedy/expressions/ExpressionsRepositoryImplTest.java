package com.github.mhewedy.expressions;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.fail;

@Slf4j
@DataJpaTest
@ContextConfiguration(classes = {JpaRepositoriesAutoConfiguration.class, DataSourceAutoConfiguration.class})
@EntityScan("com.github.mhewedy.expressions")
@EnableJpaRepositories(repositoryBaseClass = ExpressionsRepositoryImpl.class, basePackages = "com.github.mhewedy.expressions")
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
public class ExpressionsRepositoryImplTest {

    @Autowired
    private EmployeeRepository employeeRepository;

    @BeforeEach
    public void setup() {
        log.info("setting up");
        List<Employee> employees = Arrays.asList(
                new Employee(null, "ahmed", "ibrahim", LocalDate.of(1980, 10, 10), 10, Instant.parse("2007-12-03T10:15:30.00Z")),
                new Employee(null, "mohammad", "ibrahim", LocalDate.of(1985, 10, 10), 20, Instant.parse("2009-12-03T10:15:30.00Z")),
                new Employee(null, "mostafa", "ahmed", LocalDate.of(1988, 10, 10), 30, Instant.parse("2011-12-03T10:15:30.00Z")),
                new Employee(null, "wael", "ibrahim", LocalDate.of(1990, 10, 10), 40, Instant.parse("2015-12-03T10:15:30.00Z")),
                new Employee(null, "farida", "abdullah", LocalDate.of(1979, 10, 10), 50, Instant.parse("2017-12-03T10:15:30.00Z"))
        );
        employeeRepository.saveAll(employees);
    }

    @AfterEach
    public void finish() {
        log.info("cleaning");
        employeeRepository.deleteAll();
    }

    @Test
    public void testFindAllInBaseRepository() throws Exception {

        String json = loadResourceJsonFile("testFindAllInBaseRepository");

        Expressions expressions = new ObjectMapper().readValue(json, Expressions.class);

        List<Employee> employeeList = employeeRepository.findAll(expressions);
        assertThat(employeeList).isNotNull();
        assertThat(employeeList.size()).isEqualTo(1);
        assertThat(employeeList.get(0).firstName).isEqualTo("mohammad");

        // where last_name=? and birth_date>? and birth_date<=?
    }

    @Test
    public void testPagingAndSorting() throws Exception {

        String json = loadResourceJsonFile("testPagingAndSorting");

        Expressions expressions = new ObjectMapper().readValue(json, Expressions.class);

        Page<Employee> employeeList =
        employeeRepository.findAll(expressions, PageRequest.of(0, 3, Sort.by("firstName").descending()));
        assertThat(employeeList).isNotNull();
        assertThat(employeeList.getTotalElements()).isEqualTo(5);
        assertThat(employeeList.getSize()).isEqualTo(3);
        assertThat(employeeList.getContent().get(0).firstName).isEqualTo("wael");
    }

    @Test
    public void testComplexCaseWithMultipleOrAndExpressions() throws Exception {

        String json = loadResourceJsonFile("testComplexCaseWithMultipleOrAndExpressions");

        Expressions expressions = new ObjectMapper().readValue(json, Expressions.class);

        List<Employee> employeeList = employeeRepository.findAll(expressions);
        assertThat(employeeList).isNotNull();
        assertThat(employeeList.size()).isEqualTo(3);
        // where last_name = ? or first_name = ? and birth_date > ?
    }

    @Test
    public void testComplexCaseWithMultipleOrAndExpressions2() throws Exception {

        String json = loadResourceJsonFile("testComplexCaseWithMultipleOrAndExpressions2");

        Expressions expressions = new ObjectMapper().readValue(json, Expressions.class);

        List<Employee> employeeList = employeeRepository.findAll(expressions);
        assertThat(employeeList).isNotNull();
        assertThat(employeeList.size()).isEqualTo(2);
        // where last_name = ? and (first_name = ? or birth_date < ?)
    }

    @Test
    public void testUsingTheBuilderMethodOfExpressionsWithOr() throws Exception {

        String json = loadResourceJsonFile("testUsingTheBuilderMethodOfExpressionsWithOr");

        Expressions expressions = new ObjectMapper().readValue(json, Expressions.class);

        expressions.and(Expression.or(
                Expression.of("lastName", Operator.$eq, "ibrahim"),
                Expression.of("age", Operator.$in, 10, 30)
        ));

        List<Employee> employeeList = employeeRepository.findAll(expressions);
        assertThat(employeeList).isNotNull();
        assertThat(employeeList.size()).isEqualTo(3);

        // where hire_date<? and (last_name=? or age in (? , ?))
    }

    @Test
    public void testUsingTheBuilderMethodOfExpressionssWithAnd() throws Exception {

        String json = loadResourceJsonFile("testUsingTheBuilderMethodOfExpressionssWithAnd");

        Expressions expressions = new ObjectMapper().readValue(json, Expressions.class);

        expressions.or(Expression.and(
                Expression.of("lastName", Operator.$eq, "ibrahim"),
                Expression.of("age", Operator.$eq, 10)
        ));

        List<Employee> employeeList = employeeRepository.findAll(expressions);
        assertThat(employeeList).isNotNull();
        assertThat(employeeList.size()).isEqualTo(3);

        // where hire_date<? or last_name=? and age=?
    }

    @Test
    public void testSearchByEmptyObject() throws Exception {

        String json = "{}";

        Expressions expressions = new ObjectMapper().readValue(json, Expressions.class);

        List<Employee> employeeList = employeeRepository.findAll(expressions);
        assertThat(employeeList).isNotNull();
        assertThat(employeeList.size()).isEqualTo(5);

        // where ?=1
    }

    @Test
    public void testSearchByEmptyObjectAndWithDatesFromJava() throws Exception {

        String json = "{}";

        Expressions expressions = new ObjectMapper().readValue(json, Expressions.class);

        expressions.and(Expression.of("birthDate", Operator.$eq, "1980-10-10"));
        expressions.and(Expression.of("hireDate", Operator.$lte, "2007-12-03T10:15:30.00Z"));

        List<Employee> employeeList = employeeRepository.findAll(expressions);
        assertThat(employeeList).isNotNull();
        assertThat(employeeList.size()).isEqualTo(1);
        assertThat(employeeList.get(0).firstName).isEqualTo("ahmed");

        // where employee0_.hire_date=? and employee0_.birth_date=?
    }

    @Test
    public void testSearchByNull() throws Exception {

        String json = loadResourceJsonFile("testSearchByNull");

        Expressions expressions = new ObjectMapper().readValue(json, Expressions.class);

        expressions.or(Expression.of("lastName", Operator.$ne, (String) null));

        List<Employee> employeeList = employeeRepository.findAll(expressions);
        assertThat(employeeList).isNotNull();
        assertThat(employeeList.size()).isEqualTo(5);

        // where employee0_.first_name is null or employee0_.last_name is not null
    }

    @Test
    public void testSearchByContains() throws Exception {

        String json = loadResourceJsonFile("testSearchByContains");

        Expressions expressions = new ObjectMapper().readValue(json, Expressions.class);

        List<Employee> employeeList = employeeRepository.findAll(expressions);
        assertThat(employeeList).isNotNull();
        assertThat(employeeList.size()).isEqualTo(3);

        // where employee0_.first_name is null or employee0_.last_name is not null
    }

    @Test
    public void testSearchByIgnoreCaseContains() throws Exception {

        String  json = loadResourceJsonFile("testSearchByIgnoreCaseContains");

        Expressions expressions = new ObjectMapper().readValue(json, Expressions.class);

        List<Employee> employeeList = employeeRepository.findAll(expressions);
        assertThat(employeeList).isNotNull();
        assertThat(employeeList.size()).isEqualTo(3);

        // where lower(employee0_.last_name) like ?
    }

    @Test
    public void testOperatorNotSupported() throws Exception {

        String json = loadResourceJsonFile("testOperatorNotSupported");

        Expressions expressions = new ObjectMapper().readValue(json, Expressions.class);

        try {
            employeeRepository.findAll(expressions);

            fail("should throw exception");
        } catch (Exception ex) {
            assertThat(ex.getMessage())
                    .contains("No enum constant")
                    .contains("Operator.$not_supported_operator");
        }
    }

    @Test
    public void testInvalidFieldName() throws Exception {

        String json = loadResourceJsonFile("testInvalidFieldName");

        Expressions expressions = new ObjectMapper().readValue(json, Expressions.class);

        try {
            employeeRepository.findAll(expressions);

            fail("should throw exception");
        } catch (Exception ex) {
            assertThat(ex.getMessage())
                    .contains("Unable to locate SingularAttribute")
                    .contains("[invalidFieldName]");
        }
    }

    @Test
    public void testTheJavaAPI() {
        Expressions expressions = Expression.of("lastName", Operator.$eq, "ibrahim")
                .and(Expression.or(
                        Expression.of("age", Operator.$in, 10, 20),
                        Expression.of("birthDate", Operator.$lt, LocalDate.of(1980, 1, 1)))
                ).build();

        List<Employee> employeeList = employeeRepository.findAll(expressions);
        assertThat(employeeList).isNotNull();
        assertThat(employeeList.size()).isEqualTo(2);

        // where last_name=? and (age in (? , ?) or birth_date<?)
    }

    @SneakyThrows
    private String loadResourceJsonFile(String name) {
        File file = ResourceUtils.getFile("classpath:" + name + ".json");
        return new String(Files.readAllBytes(file.toPath()));
    }
}