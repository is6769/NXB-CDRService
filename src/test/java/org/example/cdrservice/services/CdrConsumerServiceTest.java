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

import static org.assertj.core.api.Assertions.assertThat;
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



    private List<Cdr> testCdrs;
    
    @BeforeEach
    void setUp() {
        testCdrs = createTestCdrs();

        ReflectionTestUtils.setField(cdrConsumerService, "numberOfRecordsInCDR", 5);
    }
    
    @Test
    @DisplayName("Should not process when there are insufficient unconsumed records")
    void consumeDataFromDB_withInsufficientRecords_shouldNotProcess() {
        when(cdrRepository.findNumberOfNonConsumedRows()).thenReturn(3);

        cdrConsumerService.consumeDataFromDB();

        verify(cdrRepository, never()).findFirstNonConsumedRecords(anyInt());
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }
    
    @Test
    @DisplayName("Should process and send to RabbitMQ when sufficient records exist")
    void consumeDataFromDB_withSufficientRecords_shouldProcessAndSendToRabbit() {
        when(cdrRepository.findNumberOfNonConsumedRows()).thenReturn(10);
        when(cdrRepository.findFirstNonConsumedRecords(anyInt())).thenReturn(testCdrs);

        cdrConsumerService.consumeDataFromDB();

        ArgumentCaptor<Object> dtoListCaptor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(eq("cdr.direct"), eq("cdr.created"), dtoListCaptor.capture());

        List<CdrDTO> sentDtos = (List<CdrDTO>) dtoListCaptor.getValue();
        assertThat(sentDtos)
            .hasSize(testCdrs.size())
            .allMatch(dto -> dto.servicedMsisdn() != null && dto.otherMsisdn() != null);

        ArgumentCaptor<Cdr> cdrCaptor = ArgumentCaptor.forClass(Cdr.class);
        verify(cdrRepository, times(testCdrs.size())).save(cdrCaptor.capture());

        assertThat(cdrCaptor.getAllValues())
            .hasSize(testCdrs.size())
            .allMatch(cdr -> cdr.getConsumedStatus() == ConsumedStatus.CONSUMED);
    }

    private List<Cdr> createTestCdrs() {
        List<Cdr> cdrs = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        for (int i = 1; i <= 5; i++) {
            cdrs.add(Cdr.builder()
                    .id((long) i)
                    .callType(i % 2 == 0 ? "01" : "02")
                    .servicedMsisdn("7900000000" + i)
                    .otherMsisdn("7900000001" + i)
                    .startDateTime(now.minusHours(i))
                    .finishDateTime(now.minusHours(i).plusMinutes(5))
                    .consumedStatus(ConsumedStatus.NEW)
                    .build());
        }
        
        return cdrs;
    }
}
