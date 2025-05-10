package org.example.cdrservice.services;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.example.cdrservice.entitites.Cdr;
import org.example.cdrservice.entitites.ConsumedStatus;
import org.example.cdrservice.entitites.Subscriber;
import org.example.cdrservice.repositories.CdrRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Slf4j
public class CdrProducerService {

    private boolean doReadyToPersist = false;

    @Value("${const.numberOfGenerationThreads}")
    private int numberOfGenerationThreads;

    private PriorityBlockingQueue<Cdr> generatedCdrsQueue = new PriorityBlockingQueue<>(8192,Comparator.comparing(Cdr::getFinishDateTime));

    private final ReentrantLock lock = new ReentrantLock();

    private final CdrRepository cdrRepository;
    private final SubscriberService subscriberService;

    public CdrProducerService(CdrRepository cdrRepository, SubscriberService subscriberService) {
        this.cdrRepository = cdrRepository;
        this.subscriberService = subscriberService;
    }

    @PostConstruct
    public void runInitialGeneration(){
        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (int i = 0; i < numberOfGenerationThreads; i++) {
            futures.add(CompletableFuture.runAsync(this::generateCdrForOneYear));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        doReadyToPersist = true;



    }

    /**
     * Генерирует случайные записи CDR за последний год.
     * Создает от 1000 до 2000 записей о звонках между абонентами.
     */
    public void generateCdrForOneYear(){

        List<Subscriber> subscribers = subscriberService.findAll();

        LocalDateTime startDateTime = LocalDateTime.now().minusYears(1);
        LocalDateTime endDateTime = LocalDateTime.now();

        long startMillis = startDateTime.atZone(ZoneId.of("Europe/Moscow")).toInstant().toEpochMilli();
        long endMillis = endDateTime.atZone(ZoneId.of("Europe/Moscow")).toInstant().toEpochMilli();

        int totalNumberOfCalls = ThreadLocalRandom.current().nextInt(1000,2001);

        for (int i = 0; i < totalNumberOfCalls; i++) {

            Cdr generatedCdr = new Cdr();


            String callType = (ThreadLocalRandom.current().nextBoolean()) ? "01" : "02";


            int randomCallerIndex = ThreadLocalRandom.current().nextInt(subscribers.size());

            int randomCalledIndex;
            do {
                randomCalledIndex = ThreadLocalRandom.current().nextInt(subscribers.size());
            }while (randomCalledIndex == randomCallerIndex);

            Subscriber caller = subscribers.get(randomCallerIndex);
            Subscriber called = subscribers.get(randomCalledIndex);


            long durationMillis = ThreadLocalRandom.current().nextLong(1,5*60*60*1000);

            long callStartMillis = ThreadLocalRandom.current().nextLong(startMillis,endMillis-durationMillis);//endMillis-durationMillis to make [l;r) maybe redo
            long callFinishMillis = callStartMillis + durationMillis;

            var callStartDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(callStartMillis),ZoneId.of("Europe/Moscow"));
            var callFinishDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(callFinishMillis),ZoneId.of("Europe/Moscow"));

            generatedCdr.setCallType(callType);
            generatedCdr.setServicedMsisdn(caller.getMsisdn());
            generatedCdr.setOtherMsisdn(called.getMsisdn());
            generatedCdr.setStartDateTime(callStartDateTime);
            generatedCdr.setFinishDateTime(callFinishDateTime);
            generatedCdr.setConsumedStatus(ConsumedStatus.NEW);

            addToDataSet(generatedCdr);
        }
    }

    private void addToDataSet(Cdr cdr){
        lock.lock();
        try {
            if (isCallAllowed(cdr.getServicedMsisdn(), cdr.getStartDateTime(), cdr.getFinishDateTime())
                    && isCallAllowed(cdr.getOtherMsisdn(), cdr.getStartDateTime(), cdr.getFinishDateTime())) {
                List<Cdr> splittedCdrs = splitIfCrossesMidnight(cdr);
                List<Cdr> mirroredSplittedCdrs = makeMirrorCdrs(splittedCdrs);
                generatedCdrsQueue.addAll(splittedCdrs);
                generatedCdrsQueue.addAll(mirroredSplittedCdrs);
            }
        }finally {
            lock.unlock();
        }
    }

    private List<Cdr> makeMirrorCdrs(List<Cdr> originalCdrs){
        List<Cdr> mirrorCdrs = new ArrayList<>();
        originalCdrs.forEach(cdr -> {
            mirrorCdrs.add(Cdr.builder()
                            .callType((cdr.getCallType().equals("01")) ? "02" : "01")
                            .servicedMsisdn(cdr.getOtherMsisdn())
                            .otherMsisdn(cdr.getServicedMsisdn())
                            .startDateTime(cdr.getStartDateTime())
                            .finishDateTime(cdr.getFinishDateTime())
                            .consumedStatus(cdr.getConsumedStatus())
                    .build());
        });
        return mirrorCdrs;
    }

    private boolean isCallAllowed(String phoneNumber, LocalDateTime newStart, LocalDateTime newFinish){
        List<Cdr> allCdrsFinishedAfterStartOfNew = generatedCdrsQueue.stream().dropWhile(cdr -> newStart.isAfter(cdr.getFinishDateTime())).toList();

        for (Cdr existing: allCdrsFinishedAfterStartOfNew){
            if ((Objects.equals(existing.getServicedMsisdn(), phoneNumber) || Objects.equals(existing.getOtherMsisdn(), phoneNumber)) &&
                    !(newFinish.isBefore(existing.getStartDateTime()) || newStart.isAfter(existing.getFinishDateTime()))) {
                return false;
            }
        }

        return true;
    }

    private List<Cdr> splitIfCrossesMidnight(Cdr cdr) {
        List<Cdr> result = new ArrayList<>();
        var currentStart = cdr.getStartDateTime();
        var currentEnd = cdr.getFinishDateTime();

        while (currentStart.isBefore(currentEnd)){
            LocalDateTime nextMidnight = currentStart.toLocalDate().plusDays(1).atStartOfDay();

            if (nextMidnight.isAfter(currentEnd)){
                result.add(new Cdr(
                        null,
                        cdr.getCallType(),
                        cdr.getServicedMsisdn(),
                        cdr.getOtherMsisdn(),
                        currentStart,
                        currentEnd,
                        cdr.getConsumedStatus()
                ));
                break;
            }

            result.add(new Cdr(
                    null,
                    cdr.getCallType(),
                    cdr.getServicedMsisdn(),
                    cdr.getOtherMsisdn(),
                    currentStart,
                    nextMidnight.minusSeconds(1),
                    cdr.getConsumedStatus()
            ));
            currentStart=nextMidnight;
        }
        return result;
    }

    @Async
    @Scheduled(fixedRateString = "${const.scheduled.produce-cdr-rate}")
    public void persistQueuedData(){
        if (!doReadyToPersist) return;
        var numberOfCdrs = ThreadLocalRandom.current().nextInt(5);
        List<Cdr> cdrsToPersist = new ArrayList<>();
        for (int i = 0; i <numberOfCdrs ; i++) {
            if (generatedCdrsQueue.isEmpty()) break;
            cdrsToPersist.add(generatedCdrsQueue.poll());
        }

        cdrRepository.saveAll(cdrsToPersist);


    }
}
