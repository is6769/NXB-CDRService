package org.example.cdrservice.services;

import org.example.cdrservice.dtos.CdrDTO;
import org.example.cdrservice.entitites.Cdr;
import org.example.cdrservice.entitites.ConsumedStatus;
import org.example.cdrservice.repositories.CdrRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CdrConsumerServiceTest {

    @Mock
    private CdrRepository cdrRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private CdrConsumerService cdrConsumerService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(cdrConsumerService, "numberOfRecordsInCDR", 5);
        ReflectionTestUtils.setField(cdrConsumerService, "CDR_EXCHANGE_NAME", "cdr.exchange");
        ReflectionTestUtils.setField(cdrConsumerService, "CDR_ROUTING_KEY", "cdr.created");
    }

    @Test
    void consumeDataFromDB_notEnoughRecords() {
        when(cdrRepository.findNumberOfNonConsumedRows()).thenReturn(4L); // Less than numberOfRecordsInCDR

        cdrConsumerService.consumeDataFromDB();

        verify(cdrRepository).findNumberOfNonConsumedRows();
        verify(cdrRepository, never()).findFirstNonConsumedRecords(anyInt());
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), anyList());
    }

    @Test
    void consumeDataFromDB_enoughRecords_sendsToRabbitAndUpdateStatus() {
        List<Cdr> mockCdrs = IntStream.range(0, 5)
                .mapToObj(i -> {
                    Cdr cdr = new Cdr();
                    cdr.setId((long) i);
                    cdr.setServicedMsisdn("7900000000" + i);
                    cdr.setOtherMsisdn("7900000001" + i);
                    cdr.setCallType(i % 2 == 0 ? "01" : "02");
                    cdr.setStartDateTime(LocalDateTime.now().minusMinutes(10 + i));
                    cdr.setFinishDateTime(LocalDateTime.now().minusMinutes(i));
                    cdr.setConsumedStatus(ConsumedStatus.NEW);
                    return cdr;
                })
                .collect(Collectors.toList());

        when(cdrRepository.findNumberOfNonConsumedRows()).thenReturn(10L); // More than numberOfRecordsInCDR
        when(cdrRepository.findFirstNonConsumedRecords(5)).thenReturn(mockCdrs);

        cdrConsumerService.consumeDataFromDB();

        verify(cdrRepository).findNumberOfNonConsumedRows();
        verify(cdrRepository).findFirstNonConsumedRecords(5);

        ArgumentCaptor<List<CdrDTO>> dtoListCaptor = ArgumentCaptor.forClass(List.class);
        verify(rabbitTemplate).convertAndSend(eq("cdr.exchange"), eq("cdr.created"), dtoListCaptor.capture());
        List<CdrDTO> sentDtos = dtoListCaptor.getValue();
        assertEquals(5, sentDtos.size());
        assertEquals(mockCdrs.get(0).getId(), sentDtos.get(0).id());


        ArgumentCaptor<Cdr> cdrCaptor = ArgumentCaptor.forClass(Cdr.class);
        verify(cdrRepository, times(5)).save(cdrCaptor.capture());
        List<Cdr> savedCdrs = cdrCaptor.getAllValues();
        for (int i = 0; i < 5; i++) {
            assertEquals(ConsumedStatus.CONSUMED, savedCdrs.get(i).getConsumedStatus());
            assertEquals(mockCdrs.get(i).getId(), savedCdrs.get(i).getId());
        }
    }
}
