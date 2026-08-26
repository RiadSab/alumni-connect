package dev.sabti.alumni_connect.alumni;

import dev.sabti.alumni_connect.auth.entities.Fields;
import dev.sabti.alumni_connect.shared.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class AlumniImportService {
    private final AlumniRecordRepository alumniRecordRepository;

    private static final List<String> REQUIRED_COLUMNS =
            List.of("student_id", "first_name", "last_name", "field_of_study", "promotion_year");

    private static final CSVFormat FORMAT = CSVFormat.DEFAULT.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .setIgnoreHeaderCase(true)
            .setIgnoreSurroundingSpaces(true)
            .setTrim(true)
            .build();

    private static final int EARLIEST_PROMOTION = 1950;

    @Transactional
    public AlumniImportResultDTO importCsv(MultipartFile file, boolean dryRun) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("The file is empty");
        }

        List<AlumniImportResultDTO.RowError> errors = new ArrayList<>();
        Set<String> seenStudentIds = new HashSet<>();
        int created = 0;
        int updated = 0;

        try (CSVParser parser = CSVParser.parse(file.getInputStream(), StandardCharsets.UTF_8, FORMAT)) {
            List<String> missing = REQUIRED_COLUMNS.stream()
                    .filter(column -> !parser.getHeaderMap().containsKey(column))
                    .toList();
            if (!missing.isEmpty()) {
                throw new BadRequestException("Missing columns: " + String.join(", ", missing));
            }

            for (CSVRecord row : parser) {
                try {
                    String studentId = required(row, "student_id");
                    if (!seenStudentIds.add(studentId)) {
                        throw new BadRequestException("Duplicate student_id in this file: " + studentId);
                    }

                    Optional<AlumniRecord> existing = alumniRecordRepository.findByStudentId(studentId);
                    AlumniRecord record = existing.orElseGet(AlumniRecord::new);
                    record.setStudentId(studentId);
                    record.setFirstName(required(row, "first_name"));
                    record.setLastName(required(row, "last_name"));
                    record.setFieldOfStudy(field(row));
                    record.setPromotionYear(promotionYear(row));
                    String email = email(row);
                    // Re-importing the school's file must not undo an opt-out.
                    if (record.getOptedOutAt() == null) record.setEmail(email);

                    if (!dryRun) alumniRecordRepository.save(record);
                    if (existing.isPresent()) updated++; else created++;
                } catch (BadRequestException e) {
                    // +1 so the number matches the line the admin sees in the file, header included.
                    errors.add(new AlumniImportResultDTO.RowError(row.getRecordNumber() + 1, e.getMessage()));
                }
            }
        } catch (IOException e) {
            throw new BadRequestException("Could not read the file");
        }

        log.info("Roster import: dryRun={} created={} updated={} errors={}", dryRun, created, updated, errors.size());
        return new AlumniImportResultDTO(dryRun, created, updated, errors);
    }

    @Transactional(readOnly = true)
    public Page<AlumniRecordDTO> getRecords(Integer promotionYear, Pageable pageable) {
        Page<AlumniRecord> page = promotionYear == null
                ? alumniRecordRepository.findAll(pageable)
                : alumniRecordRepository.findByPromotionYear(promotionYear, pageable);
        return page.map(AlumniRecordDTO::from);
    }

    private String required(CSVRecord row, String column) {
        String value = row.isSet(column) ? row.get(column) : null;
        if (value == null || value.isBlank()) {
            throw new BadRequestException(column + " is required");
        }
        return value.trim();
    }

    // The school writes "Computer Science"; the enum is COMPUTER_SCIENCE.
    private Fields field(CSVRecord row) {
        String raw = required(row, "field_of_study");
        try {
            return Fields.valueOf(raw.toUpperCase().replace(' ', '_').replace('-', '_'));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Unknown field_of_study: " + raw);
        }
    }

    private Integer promotionYear(CSVRecord row) {
        String raw = required(row, "promotion_year");
        int year;
        try {
            year = Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            throw new BadRequestException("promotion_year is not a number: " + raw);
        }
        if (year < EARLIEST_PROMOTION || year > LocalDateTime.now().getYear() + 5) {
            throw new BadRequestException("promotion_year is out of range: " + year);
        }
        return year;
    }

    // Optional: a graduate the school has no address for still counts in the denominator.
    private String email(CSVRecord row) {
        if (!row.isSet("email")) return null;
        String raw = row.get("email");
        if (raw == null || raw.isBlank()) return null;
        String email = raw.trim().toLowerCase();
        if (!email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            throw new BadRequestException("Invalid email: " + raw);
        }
        return email;
    }
}
