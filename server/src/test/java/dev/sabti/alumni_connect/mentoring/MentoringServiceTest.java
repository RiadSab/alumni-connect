package dev.sabti.alumni_connect.mentoring;

import dev.sabti.alumni_connect.alumni.AlumniRecordRepository;
import dev.sabti.alumni_connect.auth.entities.User;
import dev.sabti.alumni_connect.auth.repositories.UserRepository;
import dev.sabti.alumni_connect.candidate.CandidateProfile;
import dev.sabti.alumni_connect.candidate.CandidateProfileRepository;
import dev.sabti.alumni_connect.shared.exception.BadRequestException;
import dev.sabti.alumni_connect.shared.exception.ConflictException;
import dev.sabti.alumni_connect.shared.exception.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MentoringServiceTest {

    @Mock private MentoringSlotRepository slotRepository;
    @Mock private MentoringBookingRepository bookingRepository;
    @Mock private CandidateProfileRepository candidateProfileRepository;
    @Mock private UserRepository userRepository;
    @Mock private AlumniRecordRepository alumniRecordRepository;
    @InjectMocks private MentoringService service;

    private static final String MENTOR = "mentor@example.com";
    private static final String STUDENT = "student@example.com";

    @BeforeEach
    void injectConfiguredValues() {
        ReflectionTestUtils.setField(service, "bookingsPerSemester", 3);
        ReflectionTestUtils.setField(service, "defaultSlotMinutes", 20);
        ReflectionTestUtils.setField(service, "timezone", "Africa/Casablanca");
    }

    private CandidateProfile profile(long id, String email, boolean student) {
        User user = new User();
        user.setId(id * 10);
        user.setEmail(email);
        user.setFirstName("A");
        user.setLastName("B");
        CandidateProfile p = new CandidateProfile();
        p.setId(id);
        p.setUser(user);
        p.setIsStudent(student);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(candidateProfileRepository.findByUser(user)).thenReturn(Optional.of(p));
        return p;
    }

    // The slot's owner, when the test never looks that person up by email.
    private CandidateProfile plainProfile(long id) {
        User user = new User();
        user.setId(id * 10);
        user.setFirstName("A");
        user.setLastName("B");
        CandidateProfile p = new CandidateProfile();
        p.setId(id);
        p.setUser(user);
        return p;
    }

    private CandidateProfile asMentor() {
        CandidateProfile p = profile(1L, MENTOR, false);
        when(alumniRecordRepository.existsByClaimedBy(p.getUser())).thenReturn(true);
        return p;
    }

    private PublishSlotDTO slotDto() {
        PublishSlotDTO dto = new PublishSlotDTO();
        dto.setStartsAt(OffsetDateTime.now().plusDays(3));
        dto.setTrack(MentoringTrack.DSA);
        dto.setMeetingLink("https://meet.example.com/room");
        return dto;
    }

    private MentoringSlot openSlot(CandidateProfile mentor) {
        MentoringSlot slot = new MentoringSlot();
        slot.setId(9L);
        slot.setMentor(mentor);
        slot.setStartsAt(OffsetDateTime.now().plusDays(3));
        slot.setDurationMinutes(20);
        slot.setTrack(MentoringTrack.DSA);
        slot.setMeetingLink("https://meet.example.com/room");
        slot.setStatus(SlotStatus.OPEN);
        return slot;
    }

    // --- publishing ----------------------------------------------------------

    @Test
    void publish_withoutARosterRow_throwsForbidden() {
        User user = new User();
        user.setEmail(MENTOR);
        when(userRepository.findByEmail(MENTOR)).thenReturn(Optional.of(user));
        when(alumniRecordRepository.existsByClaimedBy(user)).thenReturn(false);

        assertThatThrownBy(() -> service.publish(MENTOR, slotDto()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Only a graduate on the school's roster can publish slots");
        verify(slotRepository, never()).save(any());
    }

    @Test
    void publish_inThePast_throwsBadRequest() {
        asMentor();
        PublishSlotDTO dto = slotDto();
        dto.setStartsAt(OffsetDateTime.now().minusHours(1));

        assertThatThrownBy(() -> service.publish(MENTOR, dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("The slot must start in the future");
    }

    @Test
    void publish_withoutAnHttpsLink_throwsBadRequest() {
        asMentor();
        PublishSlotDTO dto = slotDto();
        dto.setMeetingLink("meet.example.com/room");

        assertThatThrownBy(() -> service.publish(MENTOR, dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("The meeting link must start with https://");
    }

    @Test
    void publish_withoutADuration_usesTheConfiguredLength() {
        asMentor();
        when(slotRepository.save(any(MentoringSlot.class))).thenAnswer(inv -> inv.getArgument(0));

        MentoringSlotDTO result = service.publish(MENTOR, slotDto());

        assertThat(result.durationMinutes()).isEqualTo(20);
        assertThat(result.status()).isEqualTo(SlotStatus.OPEN);
    }

    // --- booking -------------------------------------------------------------

    @Test
    void book_asSomeoneWhoIsNotAStudent_throwsForbidden() {
        profile(2L, STUDENT, false);

        assertThatThrownBy(() -> service.book(STUDENT, 9L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Only students can book sessions");
    }

    @Test
    void book_whenTheSemesterQuotaIsUsed_throwsConflict() {
        CandidateProfile student = profile(2L, STUDENT, true);
        MentoringSlot slot = openSlot(plainProfile(1L));
        when(slotRepository.findById(9L)).thenReturn(Optional.of(slot));
        when(bookingRepository.countInWindow(eq(student), anyCollection(), any(), any())).thenReturn(3L);

        assertThatThrownBy(() -> service.book(STUDENT, 9L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("used your 3 sessions");
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void book_aSlotThatIsNoLongerOpen_throwsConflict() {
        profile(2L, STUDENT, true);
        MentoringSlot slot = openSlot(plainProfile(1L));
        slot.setStatus(SlotStatus.BOOKED);
        when(slotRepository.findById(9L)).thenReturn(Optional.of(slot));

        assertThatThrownBy(() -> service.book(STUDENT, 9L))
                .isInstanceOf(ConflictException.class)
                .hasMessage("That slot is no longer available");
    }

    @Test
    void book_marksTheSlotTakenAndReturnsTheLink() {
        CandidateProfile student = profile(2L, STUDENT, true);
        MentoringSlot slot = openSlot(plainProfile(1L));
        when(slotRepository.findById(9L)).thenReturn(Optional.of(slot));
        when(bookingRepository.countInWindow(eq(student), anyCollection(), any(), any())).thenReturn(1L);
        when(bookingRepository.save(any(MentoringBooking.class))).thenAnswer(inv -> inv.getArgument(0));

        MentoringBookingDTO result = service.book(STUDENT, 9L);

        assertThat(slot.getStatus()).isEqualTo(SlotStatus.BOOKED);
        assertThat(result.state()).isEqualTo(BookingState.BOOKED);
        // The two parties get the link; the browse list never does.
        assertThat(result.meetingLink()).isEqualTo("https://meet.example.com/room");
    }

    @Test
    void cancelBooking_putsTheSlotBackInThePool() {
        CandidateProfile student = profile(2L, STUDENT, true);
        MentoringSlot slot = openSlot(plainProfile(1L));
        slot.setStatus(SlotStatus.BOOKED);
        MentoringBooking booking = new MentoringBooking();
        booking.setId(4L);
        booking.setSlot(slot);
        booking.setStudent(student);
        booking.setState(BookingState.BOOKED);
        when(bookingRepository.findByIdAndStudent(4L, student)).thenReturn(Optional.of(booking));

        service.cancelBooking(STUDENT, 4L);

        assertThat(booking.getState()).isEqualTo(BookingState.CANCELLED_BY_STUDENT);
        assertThat(booking.getCancelledAt()).isNotNull();
        assertThat(slot.getStatus()).isEqualTo(SlotStatus.OPEN);
    }
}
