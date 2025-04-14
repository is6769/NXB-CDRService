package org.example.cdrservice.dtos;

import org.example.cdrservice.entitites.Cdr;

import java.time.LocalDateTime;

public record CdrDTO (
        Long id,
        String callType,
        String callerNumber,
        String calledNumber,
        LocalDateTime startDateTime,
        LocalDateTime finishDateTime
){
    public static CdrDTO createFromEntity(Cdr cdr){
        return new CdrDTO(
                cdr.getId(),
                cdr.getCallType(),
                cdr.getCallerNumber(),
                cdr.getCalledNumber(),
                cdr.getStartDateTime(),
                cdr.getFinishDateTime()
        );
    }
}
