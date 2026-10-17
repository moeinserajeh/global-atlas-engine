package com.globalatlas.health;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {
 @ExceptionHandler(ResponseStatusException.class)
 public ResponseEntity<Map<String,Object>> handleStatus(ResponseStatusException ex) {
  HttpStatus status=HttpStatus.valueOf(ex.getStatusCode().value());
  return ResponseEntity.status(status).body(body(status, safe(ex.getReason(),status)));
 }
 @ExceptionHandler(Exception.class)
 public ResponseEntity<Map<String,Object>> handleUnexpected(Exception ex) {
  HttpStatus status=HttpStatus.INTERNAL_SERVER_ERROR;
  return ResponseEntity.status(status).body(body(status,"An unexpected server error occurred."));
 }
 private static Map<String,Object> body(HttpStatus status,String message) {
  Map<String,Object> out=new LinkedHashMap<>();
  out.put("timestamp",Instant.now().toString()); out.put("status",status.value());
  out.put("error",status.getReasonPhrase()); out.put("message",message); return out;
 }
 private static String safe(String reason,HttpStatus status) {
  if(reason==null||reason.isBlank()) return status.getReasonPhrase();
  return reason.length()>200?reason.substring(0,200):reason;
 }
}
