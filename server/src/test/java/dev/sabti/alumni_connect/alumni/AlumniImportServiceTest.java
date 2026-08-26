package dev.sabti.alumni_connect.alumni;

import dev.sabti.alumni_connect.auth.entities.Fields;
import dev.sabti.alumni_connect.shared.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlumniImportServiceTest {

    @Mock private AlumniRecordRepository alumniRecordRepository;
    @InjectMocks private AlumniImportService service;

    private static final String HEADER = "student_id,first_name,last_name,field_of_study,promotion_year,email\n";

    private MultipartFile csv(String body) {
        return new MockMultipartFile("file", "roster.csv", "text/csv",
                (HEADER + body).getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void import_newRows_areCreated() {
        when(alumniRecordRepository.findByStudentId(anyString())).thenReturn(Optional.empty());

        AlumniImportResultDTO result = service.importCsv(
                csv("2201,Yasmine,Alaoui,Computer Science,2024,yasmine@example.com\n"), false);

        assertThat(result.created()).isEqualTo(1);
        assertThat(result.updated()).isZero();
        assertThat(result.errors()).isEmpty();

        ArgumentCaptor<AlumniRecord> saved = ArgumentCaptor.forClass(AlumniRecord.class);
        verify(alumniRecordRepository).save(saved.capture());
        assertThat(saved.getValue().getStudentId()).isEqualTo("2201");
        assertThat(saved.getValue().getFieldOfStudy()).isEqualTo(Fields.COMPUTER_SCIENCE);
        assertThat(saved.getValue().getPromotionYear()).isEqualTo(2024);
        assertThat(saved.getValue().getEmail()).isEqualTo("yasmine@example.com");
    }

    @Test
    void import_knownStudentId_updatesInsteadOfDuplicating() {
        AlumniRecord existing = new AlumniRecord();
        existing.setStudentId("2201");
        existing.setLastName("Alaoui");
        when(alumniRecordRepository.findByStudentId("2201")).thenReturn(Optional.of(existing));

        AlumniImportResultDTO result = service.importCsv(
                csv("2201,Yasmine,Alaoui-Idrissi,Data Science,2024,\n"), false);

        assertThat(result.created()).isZero();
        assertThat(result.updated()).isEqualTo(1);
        assertThat(existing.getLastName()).isEqualTo("Alaoui-Idrissi");
        assertThat(existing.getEmail()).isNull();
    }

    // The reason this reads CSV properly instead of splitting on commas.
    @Test
    void import_quotedNameContainingAComma_isOneField() {
        when(alumniRecordRepository.findByStudentId(anyString())).thenReturn(Optional.empty());

        service.importCsv(csv("2202,Mohammed,\"El Fassi, Jr\",Physics,2023,\n"), false);

        ArgumentCaptor<AlumniRecord> saved = ArgumentCaptor.forClass(AlumniRecord.class);
        verify(alumniRecordRepository).save(saved.capture());
        assertThat(saved.getValue().getLastName()).isEqualTo("El Fassi, Jr");
    }

    @Test
    void import_badRows_areReportedAndTheRestStillImport() {
        when(alumniRecordRepository.findByStudentId(anyString())).thenReturn(Optional.empty());

        AlumniImportResultDTO result = service.importCsv(csv("""
                2203,Sara,Bennani,Astrology,2024,
                2204,,Cherkaoui,Physics,2024,
                2205,Omar,Idrissi,Physics,not-a-year,
                2206,Nadia,Tazi,Physics,2024,nadia@example.com
                """), false);

        assertThat(result.created()).isEqualTo(1);
        assertThat(result.errors()).hasSize(3);
        assertThat(result.errors().get(0).message()).contains("Unknown field_of_study: Astrology");
        assertThat(result.errors().get(1).message()).contains("first_name is required");
        assertThat(result.errors().get(2).message()).contains("promotion_year is not a number");
        // Line numbers point at the file as the admin sees it, header included.
        assertThat(result.errors().get(0).line()).isEqualTo(2);
        assertThat(result.errors().get(2).line()).isEqualTo(4);
    }

    @Test
    void import_sameStudentIdTwiceInOneFile_isAnError() {
        when(alumniRecordRepository.findByStudentId("2207")).thenReturn(Optional.empty());

        AlumniImportResultDTO result = service.importCsv(csv("""
                2207,Salma,Rachidi,Physics,2024,
                2207,Salma,Rachidi,Physics,2024,
                """), false);

        assertThat(result.created()).isEqualTo(1);
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).message()).contains("Duplicate student_id");
    }

    @Test
    void import_dryRun_writesNothing() {
        when(alumniRecordRepository.findByStudentId(anyString())).thenReturn(Optional.empty());

        AlumniImportResultDTO result = service.importCsv(
                csv("2208,Hamza,Berrada,Physics,2024,\n"), true);

        assertThat(result.dryRun()).isTrue();
        assertThat(result.created()).isEqualTo(1);
        verify(alumniRecordRepository, never()).save(any());
    }

    @Test
    void import_missingColumn_throwsBadRequest() {
        MultipartFile file = new MockMultipartFile("file", "roster.csv", "text/csv",
                "student_id,first_name\n2209,Adam\n".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> service.importCsv(file, false))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Missing columns: last_name, field_of_study, promotion_year");
    }

    @Test
    void import_emptyFile_throwsBadRequest() {
        MultipartFile file = new MockMultipartFile("file", "roster.csv", "text/csv", new byte[0]);

        assertThatThrownBy(() -> service.importCsv(file, false))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("The file is empty");
    }
}
