package org.example.cdrservice.dtos;

import org.example.cdrservice.entitites.Cdr;

import java.time.LocalDateTime;

public record CdrDTO (
        String callType,
        String servicedMsisdn,
        String otherMsisdn,
        LocalDateTime startDateTime,
        LocalDateTime finishDateTime
){
    public static CdrDTO createFromEntity(Cdr cdr){
        return new CdrDTO(
                cdr.getCallType(),
                cdr.getServicedMsisdn(),
                cdr.getOtherMsisdn(),
                cdr.getStartDateTime(),
                cdr.getFinishDateTime()
        );
    }
}
