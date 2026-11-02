package mn.edu.must.sqat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseRegistrationServiceTest {

    private static final String STUDENT_ID = "B232270012";
    private static final String COURSE_ID = "F.CSA313";
    private static final String EMAIL = "student@example.com";
    private static final String SUBJECT = "Бүртгэл баталгаажлаа";

    @Mock
    private StudentRepository repository;

    @Mock
    private EmailSender emailSender;

    private CourseRegistrationService service;

    @BeforeEach
    void setUp() {
        service = new CourseRegistrationService(repository, emailSender);
    }

    @Test
    @DisplayName("A1: active student is registered and notified")
    void register_activeStudentWithPrerequisites_returnsOkAndNotifies() {
        Student student = new Student(STUDENT_ID, EMAIL, true, List.of("F.CSA211"));
        when(repository.findStudent(STUDENT_ID)).thenReturn(student);
        when(repository.courseExists(COURSE_ID)).thenReturn(true);
        when(repository.prerequisites(COURSE_ID)).thenReturn(List.of("F.CSA211"));

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals("OK", result);
        verify(repository).saveRegistration(STUDENT_ID, COURSE_ID);
        verify(emailSender).send(EMAIL, SUBJECT,
                COURSE_ID + " хичээлд амжилттай бүртгэгдлээ.");
    }

    @Test
    @DisplayName("A2: missing student is neither registered nor notified")
    void register_missingStudent_returnsNoStudentWithoutSavingOrEmail() {
        when(repository.findStudent(STUDENT_ID)).thenReturn(null);

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals("ERROR_NO_STUDENT", result);
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verify(emailSender, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("A3: inactive student is rejected without email interactions")
    void register_inactiveStudent_returnsInactiveWithoutSavingOrEmail() {
        Student student = new Student(STUDENT_ID, EMAIL, false, List.of());
        when(repository.findStudent(STUDENT_ID)).thenReturn(student);

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals("ERROR_INACTIVE_STUDENT", result);
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verifyNoInteractions(emailSender);
    }

    @Test
    @DisplayName("A4: missing prerequisite prevents registration")
    void register_missingPrerequisite_returnsPrerequisitesWithoutSavingOrEmail() {
        Student student = new Student(STUDENT_ID, EMAIL, true, List.of("F.CSA211"));
        when(repository.findStudent(STUDENT_ID)).thenReturn(student);
        when(repository.courseExists(COURSE_ID)).thenReturn(true);
        when(repository.prerequisites(COURSE_ID))
                .thenReturn(List.of("F.CSA211", "F.CSA212"));

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals("ERROR_PREREQUISITES", result);
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verifyNoInteractions(emailSender);
    }

    @Test
    @DisplayName("A5: email failure does not cancel a saved registration")
    void register_emailSenderThrows_returnsOkAndKeepsRegistration() {
        Student student = new Student(STUDENT_ID, EMAIL, true, List.of());
        when(repository.findStudent(STUDENT_ID)).thenReturn(student);
        when(repository.courseExists(COURSE_ID)).thenReturn(true);
        when(repository.prerequisites(COURSE_ID)).thenReturn(List.of());
        doThrow(new RuntimeException("SMTP unavailable"))
                .when(emailSender).send(anyString(), anyString(), anyString());

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals("OK", result);
        verify(repository).saveRegistration(STUDENT_ID, COURSE_ID);
        verify(emailSender).send(eq(EMAIL), eq(SUBJECT), anyString());
    }

    @Test
    @DisplayName("A6: either null argument is rejected before accessing dependencies")
    void register_eitherArgumentNull_throwsWithoutDependencyInteractions() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> service.register(null, COURSE_ID)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> service.register(STUDENT_ID, null)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> service.register(null, null)));

        verifyNoInteractions(repository, emailSender);
    }

    @Test
    @DisplayName("A7: captured email body contains the registered course code")
    void register_successfulRegistration_includesCourseCodeInEmailBody() {
        Student student = new Student(STUDENT_ID, EMAIL, true, List.of());
        when(repository.findStudent(STUDENT_ID)).thenReturn(student);
        when(repository.courseExists(COURSE_ID)).thenReturn(true);
        when(repository.prerequisites(COURSE_ID)).thenReturn(List.of());
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals("OK", result);
        verify(emailSender).send(eq(EMAIL), eq(SUBJECT), body.capture());
        assertTrue(body.getValue().contains(COURSE_ID),
                "The confirmation body must contain the registered course code");
    }

    @DisplayName("A8: three invalid registration classes")
    @ParameterizedTest(name = "[{index}] expected={3}")
    @CsvSource({
            "false, true, true, ERROR_NO_STUDENT",
            "true, false, true, ERROR_INACTIVE_STUDENT",
            "true, true, false, ERROR_NO_COURSE"
    })
    void register_invalidStudentOrCourse_returnsExpectedErrorWithoutSideEffects(
            boolean studentExists, boolean active, boolean courseExists, String expected) {
        Student student = studentExists
                ? new Student(STUDENT_ID, EMAIL, active, List.of()) : null;
        when(repository.findStudent(STUDENT_ID)).thenReturn(student);
        // The first two rows return before checking whether the course exists.
        lenient().when(repository.courseExists(COURSE_ID)).thenReturn(courseExists);

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals(expected, result);
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verifyNoInteractions(emailSender);
    }
}
