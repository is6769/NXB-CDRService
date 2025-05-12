package org.example.cdrservice.services;

import org.example.cdrservice.entitites.Cdr;
import org.example.cdrservice.entitites.ConsumedStatus;
import org.example.cdrservice.entitites.Subscriber;
import org.example.cdrservice.repositories.CdrRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * Тестовый класс для {@link CdrProducerService}.
 * Проверяет корректность генерации, обработки и сохранения CDR.
 */
@ExtendWith(MockitoExtension.class)
class CdrProducerServiceTest {

    @Mock
    private CdrRepository cdrRepository;

    @Mock
    private SubscriberService subscriberService;

    @InjectMocks
    private CdrProducerService cdrProducerService;

    @Captor
    private ArgumentCaptor<List<Cdr>> cdrListCaptor;

    /**
     * Тестирует корректность разделения CDR, пересекающих полночь, методом {@code splitIfCrossesMidnight}.
     * Ожидается, что CDR будет разделен на две части: до полуночи и после.
     * @throws Exception если возникает ошибка при вызове приватного метода через рефлексию.
     */
    @Test
    @DisplayName("splitIfCrossesMidnight должен корректно разделять CDR, пересекающие полночь")
    void splitIfCrossesMidnight_shouldCorrectlySplitCdrsCrossingMidnight() throws Exception {
        LocalDateTime todayEvening = LocalDateTime.of(LocalDate.now(), LocalTime.of(23, 45, 0));
        LocalDateTime tomorrowMorning = todayEvening.plusHours(2);

        Cdr testCdr = Cdr.builder()
                .callType("01")
                .servicedMsisdn("79000000001")
                .otherMsisdn("79000000002")
                .startDateTime(todayEvening)
                .finishDateTime(tomorrowMorning)
                .consumedStatus(ConsumedStatus.NEW)
                .build();

        Method splitMethod = CdrProducerService.class.getDeclaredMethod("splitIfCrossesMidnight", Cdr.class);
        splitMethod.setAccessible(true);

        List<Cdr> result = (List<Cdr>) splitMethod.invoke(cdrProducerService, testCdr);

        assertThat(result).hasSize(2)
            .extracting(Cdr::getStartDateTime, Cdr::getFinishDateTime)
            .containsExactly(
                tuple(todayEvening, LocalDateTime.of(LocalDate.now(), LocalTime.of(23, 59, 59))),
                tuple(LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.MIDNIGHT), tomorrowMorning)
            );
    }

    /**
     * Тестирует обработку CDR, охватывающих несколько дней, методом {@code splitIfCrossesMidnight}.
     * Ожидается, что CDR будет разделен на соответствующее количество частей по дням.
     * @throws Exception если возникает ошибка при вызове приватного метода через рефлексию.
     */
    @Test
    @DisplayName("splitIfCrossesMidnight должен обрабатывать CDR, охватывающие несколько дней")
    void splitIfCrossesMidnight_shouldHandleCdrsSpanningMultipleDays() throws Exception {
        LocalDate today = LocalDate.now();
        LocalDateTime start = LocalDateTime.of(today, LocalTime.of(22, 0));
        LocalDateTime end = LocalDateTime.of(today.plusDays(2), LocalTime.of(10, 0));

        Cdr testCdr = Cdr.builder()
                .callType("01")
                .servicedMsisdn("79000000001")
                .otherMsisdn("79000000002")
                .startDateTime(start)
                .finishDateTime(end)
                .consumedStatus(ConsumedStatus.NEW)
                .build();

        Method splitMethod = CdrProducerService.class.getDeclaredMethod("splitIfCrossesMidnight", Cdr.class);
        splitMethod.setAccessible(true);

        List<Cdr> result = (List<Cdr>) splitMethod.invoke(cdrProducerService, testCdr);


        assertThat(result).hasSize(3)
            .extracting(Cdr::getStartDateTime)
            .satisfies(startTimes -> {
                assertThat(startTimes.get(0).toLocalDate()).isEqualTo(today);
                assertThat(startTimes.get(1).toLocalDate()).isEqualTo(today.plusDays(1));
                assertThat(startTimes.get(2).toLocalDate()).isEqualTo(today.plusDays(2));
            });
    }

    /**
     * Тестирует создание корректных зеркальных записей методом {@code makeMirrorCdrs}.
     * Проверяет, что тип вызова инвертируется, а номера абонентов меняются местами.
     * @throws Exception если возникает ошибка при вызове приватного метода через рефлексию.
     */
    @Test
    @DisplayName("makeMirrorCdrs должен создавать корректные зеркальные записи")
    void makeMirrorCdrs_shouldCreateCorrectMirroredRecords() throws Exception {
        List<Cdr> originalCdrs = Arrays.asList(
            Cdr.builder()
                .callType("01")
                .servicedMsisdn("79000000001")
                .otherMsisdn("79000000002")
                .startDateTime(LocalDateTime.now().minusHours(1))
                .finishDateTime(LocalDateTime.now())
                .consumedStatus(ConsumedStatus.NEW)
                .build(),
            Cdr.builder()
                .callType("02")
                .servicedMsisdn("79000000003")
                .otherMsisdn("79000000004")
                .startDateTime(LocalDateTime.now().minusHours(2))
                .finishDateTime(LocalDateTime.now().minusHours(1))
                .consumedStatus(ConsumedStatus.NEW)
                .build()
        );


        Method mirrorMethod = CdrProducerService.class.getDeclaredMethod("makeMirrorCdrs", List.class);
        mirrorMethod.setAccessible(true);

        List<Cdr> mirroredCdrs = (List<Cdr>) mirrorMethod.invoke(cdrProducerService, originalCdrs);

        assertThat(mirroredCdrs).hasSize(2);

        assertThat(mirroredCdrs.get(0))
            .extracting(
                Cdr::getCallType,
                Cdr::getServicedMsisdn,
                Cdr::getOtherMsisdn
            )
            .containsExactly(
                "02",
                "79000000002",
                "79000000001"
            );

        assertThat(mirroredCdrs.get(1))
            .extracting(
                Cdr::getCallType,
                Cdr::getServicedMsisdn,
                Cdr::getOtherMsisdn
            )
            .containsExactly(
                "01",
                "79000000004",
                "79000000003"
            );

        // Verify timestamps remain the same
        for (int i = 0; i < originalCdrs.size(); i++) {
            assertThat(mirroredCdrs.get(i).getStartDateTime()).isEqualTo(originalCdrs.get(i).getStartDateTime());
            assertThat(mirroredCdrs.get(i).getFinishDateTime()).isEqualTo(originalCdrs.get(i).getFinishDateTime());
        }
    }

    /**
     * Тестирует обнаружение пересекающихся вызовов для одного и того же абонента методом {@code isCallAllowed}.
     * Проверяет различные сценарии пересечения и отсутствия пересечения вызовов.
     * @throws Exception если возникает ошибка при вызове приватного метода через рефлексию.
     */
    @Test
    @DisplayName("isCallAllowed должен обнаруживать пересекающиеся вызовы для одного абонента")
    void isCallAllowed_shouldDetectOverlappingCalls() throws Exception {
        PriorityBlockingQueue<Cdr> testQueue = new PriorityBlockingQueue<>(10,
                Comparator.comparing(Cdr::getFinishDateTime));

        LocalDateTime existingStart = LocalDateTime.of(LocalDate.now(), LocalTime.of(10, 0));
        LocalDateTime existingEnd = LocalDateTime.of(LocalDate.now(), LocalTime.of(10, 30));
        Cdr existingCdr = Cdr.builder()
                .callType("01")
                .servicedMsisdn("79000000001")
                .otherMsisdn("79000000002")
                .startDateTime(existingStart)
                .finishDateTime(existingEnd)
                .consumedStatus(ConsumedStatus.NEW)
                .build();
        testQueue.add(existingCdr);

        ReflectionTestUtils.setField(cdrProducerService, "generatedCdrsQueue", testQueue);

        Method isCallAllowedMethod = CdrProducerService.class.getDeclaredMethod(
                "isCallAllowed", String.class, LocalDateTime.class, LocalDateTime.class);
        isCallAllowedMethod.setAccessible(true);

        LocalDateTime newStart1 = LocalDateTime.of(LocalDate.now(), LocalTime.of(10, 15));
        LocalDateTime newEnd1 = LocalDateTime.of(LocalDate.now(), LocalTime.of(10, 20));
        Boolean result1 = (Boolean) isCallAllowedMethod.invoke(
                cdrProducerService, "79000000001", newStart1, newEnd1);

        LocalDateTime newStart2 = LocalDateTime.of(LocalDate.now(), LocalTime.of(9, 45));
        LocalDateTime newEnd2 = LocalDateTime.of(LocalDate.now(), LocalTime.of(10, 15));
        Boolean result2 = (Boolean) isCallAllowedMethod.invoke(
                cdrProducerService, "79000000001", newStart2, newEnd2);

        LocalDateTime newStart3 = LocalDateTime.of(LocalDate.now(), LocalTime.of(10, 15));
        LocalDateTime newEnd3 = LocalDateTime.of(LocalDate.now(), LocalTime.of(10, 45));
        Boolean result3 = (Boolean) isCallAllowedMethod.invoke(
                cdrProducerService, "79000000001", newStart3, newEnd3);

        LocalDateTime newStart4 = LocalDateTime.of(LocalDate.now(), LocalTime.of(9, 0));
        LocalDateTime newEnd4 = LocalDateTime.of(LocalDate.now(), LocalTime.of(9, 30));
        Boolean result4 = (Boolean) isCallAllowedMethod.invoke(
                cdrProducerService, "79000000001", newStart4, newEnd4);


        LocalDateTime newStart5 = LocalDateTime.of(LocalDate.now(), LocalTime.of(11, 0));
        LocalDateTime newEnd5 = LocalDateTime.of(LocalDate.now(), LocalTime.of(11, 30));
        Boolean result5 = (Boolean) isCallAllowedMethod.invoke(
                cdrProducerService, "79000000001", newStart5, newEnd5);


        LocalDateTime newStart6 = LocalDateTime.of(LocalDate.now(), LocalTime.of(10, 15));
        LocalDateTime newEnd6 = LocalDateTime.of(LocalDate.now(), LocalTime.of(10, 20));
        Boolean result6 = (Boolean) isCallAllowedMethod.invoke(
                cdrProducerService, "79000000003", newStart6, newEnd6);


        assertThat(result1).isFalse();
        assertThat(result2).isFalse();
        assertThat(result3).isFalse();
        assertThat(result4).isTrue();
        assertThat(result5).isTrue();
        assertThat(result6).isTrue();
    }

    /**
     * Тестирует метод {@code generateCdrForOneYear}.
     * Проверяет, что CDR создаются, помещаются в очередь и что разрешается их последующее сохранение.
     * Также проверяются основные атрибуты сгенерированных CDR.
     */
    @Test
    @DisplayName("generateCdrForOneYear должен создавать CDR, помещать их в очередь и разрешать сохранение")
    void generateCdrForOneYear_shouldCreateCdrsForOneYear() {
        Subscriber subscriber1 = new Subscriber(1L, "79001111111");
        Subscriber subscriber2 = new Subscriber(2L, "79002222222");
        List<Subscriber> subscribers = Arrays.asList(subscriber1, subscriber2);

        when(subscriberService.findAll()).thenReturn(subscribers);

        PriorityBlockingQueue<Cdr> cdrQueue = new PriorityBlockingQueue<>(10000,
            Comparator.comparing(Cdr::getFinishDateTime));
        ReflectionTestUtils.setField(cdrProducerService, "generatedCdrsQueue", cdrQueue);

        when(cdrRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<Cdr> cdrs = invocation.getArgument(0);
            return cdrs;
        });
        
        cdrProducerService.generateCdrForOneYear();

        ReflectionTestUtils.setField(cdrProducerService, "doReadyToPersist", true);
        cdrProducerService.persistQueuedData();
        
        verify(cdrRepository, atLeastOnce()).saveAll(cdrListCaptor.capture());
        
        List<Cdr> allSavedCdrs = cdrListCaptor.getAllValues().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());


        LocalDateTime earliest = allSavedCdrs.stream()
                .map(Cdr::getStartDateTime)
                .min(LocalDateTime::compareTo)
                .orElseThrow(() -> new AssertionError("Could not determine earliest CDR start time."));
                
        LocalDateTime latest = allSavedCdrs.stream()
                .map(Cdr::getFinishDateTime)
                .max(LocalDateTime::compareTo)
                .orElseThrow(() -> new AssertionError("Could not determine latest CDR finish time."));
                
        long daysBetween = ChronoUnit.DAYS.between(earliest.toLocalDate(), latest.toLocalDate());
        
        LocalDateTime oneYearAgo = LocalDateTime.now().minusYears(1);
        long daysDifference = Math.abs(ChronoUnit.DAYS.between(earliest.toLocalDate(), oneYearAgo.toLocalDate()));
        
        assertThat(daysBetween).as("CDRs should span close to a full year").isGreaterThanOrEqualTo(0);
        assertThat(daysDifference).as("Earliest CDR should be close to one year ago").isLessThanOrEqualTo(366);
        
        allSavedCdrs.forEach(cdr -> {
            assertThat(cdr.getCallType()).isIn("01", "02");
            assertThat(cdr.getServicedMsisdn()).isNotNull();
            assertThat(cdr.getOtherMsisdn()).isNotNull();
            assertThat(cdr.getStartDateTime()).isNotNull();
            assertThat(cdr.getFinishDateTime()).isNotNull();
            assertThat(cdr.getConsumedStatus()).isEqualTo(ConsumedStatus.NEW);
            
            assertThat(cdr.getFinishDateTime()).isAfter(cdr.getStartDateTime());
            
            assertThat(cdr.getServicedMsisdn()).isIn("79001111111", "79002222222");
            assertThat(cdr.getOtherMsisdn()).isIn("79001111111", "79002222222");
            
            assertThat(cdr.getServicedMsisdn()).isNotEqualTo(cdr.getOtherMsisdn());
        });
    }
}
