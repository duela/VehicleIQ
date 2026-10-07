package uk.vehicleiq.core;

import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class) ResponseEntity<Map<String,Object>> status(ResponseStatusException e){
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("error",Map.of("code",e.getStatusCode().toString(),"message",Objects.toString(e.getReason(),"Request failed")),"requestId",UUID.randomUUID().toString()));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<Map<String,Object>> invalid(MethodArgumentNotValidException e){
        List<String> messages=e.getBindingResult().getFieldErrors().stream().map(x->x.getField()+": "+x.getDefaultMessage()).toList();
        return ResponseEntity.badRequest().body(Map.of("error",Map.of("code","VALIDATION_FAILED","message","Please check the submitted fields.","details",messages),"requestId",UUID.randomUUID().toString()));
    }
    @ExceptionHandler(NoSuchElementException.class) ResponseEntity<Map<String,Object>> missing(NoSuchElementException e){return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error",Map.of("code","NOT_FOUND","message",e.getMessage()),"requestId",UUID.randomUUID().toString()));}
}

