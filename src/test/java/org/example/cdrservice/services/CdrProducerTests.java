package org.example.cdrservice.services;

import org.example.cdrservice.entitites.Cdr;
import org.example.cdrservice.entitites.ConsumedStatus;
import org.example.cdrservice.entitites.Subscriber;
import org.example.cdrservice.repositories.CdrRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CdrProducerTests {

    @Mock
    private SubscriberService subscriberService;

    @Mock
    private CdrRepository cdrRepository;

    @Spy
    @InjectMocks
    private CdrProducerService cdrProducerService;

    private ConcurrentSkipListSet<Cdr> generatedCdrSet;
    private List<Subscriber> testSubscribers;

    @BeforeEach
    void setUp() throws Exception {
        // Prepare test subscribers
        testSubscribers = createTestSubscribers();
        when(subscriberService.findAll()).thenReturn(testSubscribers);
        
        // Set a small number of threads for testing
        ReflectionTestUtils.setField(cdrProducerService, "numberOfGenerationThreads", 3);
        
        // Get access to the generated CDR set
        Field generatedCdrSetField = CdrProducerService.class.getDeclaredField("generatedCdrSet");
        generatedCdrSetField.setAccessible(true);
        generatedCdrSet = (ConcurrentSkipListSet<Cdr>) generatedCdrSetField.get(cdrProducerService);
    }

    @Test
    void testMultiThreadedGenerationHasNoOverlappingCalls() throws Exception {
        // Run the data generation
        cdrProducerService.runInitialGeneration();
        
        // Verify we have data generated
        assertFalse(generatedCdrSet.isEmpty(), "CDR set should not be empty after generation");
        
        // Group CDRs by phone number
        Map<String, List<Cdr>> callsByPhoneNumber = new HashMap<>();
        
        // Process each CDR
        for (Cdr cdr : generatedCdrSet) {
            // Add to caller's list
            callsByPhoneNumber
                .computeIfAbsent(cdr.getServicedMsisdn(), k -> new ArrayList<>())
                .add(cdr);
            
            // Add to called's list
            callsByPhoneNumber
                .computeIfAbsent(cdr.getOtherMsisdn(), k -> new ArrayList<>())
                .add(cdr);
        }
        
        // Test that no phone number has overlapping calls
        boolean noOverlapsFound = true;
        StringBuilder errorDetails = new StringBuilder();
        
        for (Map.Entry<String, List<Cdr>> entry : callsByPhoneNumber.entrySet()) {
            String phoneNumber = entry.getKey();
            List<Cdr> calls = entry.getValue();
            
            // Sort calls by start time
            calls.sort(Comparator.comparing(Cdr::getStartDateTime));
            
            // Check for overlaps
            for (int i = 0; i < calls.size() - 1; i++) {
                for (int j = i + 1; j < calls.size(); j++) {
                    Cdr call1 = calls.get(i);
                    Cdr call2 = calls.get(j);
                    
                    LocalDateTime call1Start = call1.getStartDateTime();
                    LocalDateTime call1End = call1.getFinishDateTime();
                    LocalDateTime call2Start = call2.getStartDateTime();
                    LocalDateTime call2End = call2.getFinishDateTime();
                    
                    // Check if calls overlap
                    boolean callsOverlap = !(call1End.isBefore(call2Start) || call1Start.isAfter(call2End));
                    
                    if (callsOverlap) {
                        noOverlapsFound = false;
                        errorDetails.append(String.format(
                            "Found overlapping calls for number %s:\n" +
                            "  Call 1: %s → %s (caller: %s, called: %s)\n" +
                            "  Call 2: %s → %s (caller: %s, called: %s)\n\n",
                            phoneNumber,
                            call1.getStartDateTime(), call1.getFinishDateTime(), 
                            call1.getServicedMsisdn(), call1.getOtherMsisdn(),
                            call2.getStartDateTime(), call2.getFinishDateTime(),
                            call2.getServicedMsisdn(), call2.getOtherMsisdn()
                        ));
                    }
                }
            }
        }
        
        // Assert no overlaps were found with detailed error message
        assertTrue(noOverlapsFound, 
            "Found overlapping calls in the generated data set:\n" + errorDetails.toString());
        
        // Log summary
        System.out.println("✓ Test passed - No overlapping calls detected");
        System.out.println("Total generated CDRs: " + generatedCdrSet.size());
        System.out.println("Total unique phone numbers: " + callsByPhoneNumber.size());
    }
    
    private List<Subscriber> createTestSubscribers() {
        List<Subscriber> subscribers = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            Subscriber subscriber = new Subscriber();
            subscriber.setId((long) i);
            subscriber.setMsisdn("7900000000" + i);
            subscribers.add(subscriber);
        }
        return subscribers;
    }
}
