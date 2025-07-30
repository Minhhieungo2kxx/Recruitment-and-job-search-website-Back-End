package com.webjob.application.Utils.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        ErrorResponseVadidate<Object> errorResponseVadidate = new ErrorResponseVadidate<>(
                HttpStatus.BAD_REQUEST.value(),
                "Dữ liệu không hợp lệ",
                LocalDateTime.now(),
                errors,
                null
        );

        return new ResponseEntity<>(errorResponseVadidate, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<?> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        ErrorResponException<?> errorResponException=new ErrorResponException<>(
                HttpStatus.BAD_REQUEST.value(),
                "Exception Error",
                LocalDateTime.now(),
                ex.getMessage(),
                null
        );

        return new ResponseEntity<>(ResponseEntity.badRequest(), HttpStatus.BAD_REQUEST);
    }

    // ✅ 3. Xử lý HttpMessageNotReadableException (khi JSON bị sai format hoặc thiếu)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        ErrorResponException<?> errorResponException=new ErrorResponException<>(
                HttpStatus.BAD_REQUEST.value(),
                "Exception Error",
                LocalDateTime.now(),
                ex.getMessage(),
                null
        );
        return new ResponseEntity<>(errorResponException, HttpStatus.BAD_REQUEST);
    }

  //   ✅ 4. Xử lý NullPointerException (lỗi lập trình)
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<?> handleNullPointerException(NullPointerException ex) {
        ErrorResponException<?> errorResponException=new ErrorResponException<>(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Exception Error",
                LocalDateTime.now(),
                ex.getMessage(),
                null
        );
        return new ResponseEntity<>(errorResponException, HttpStatus.INTERNAL_SERVER_ERROR);
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGenericException(Exception ex) {
        ErrorResponException<?> errorResponException=new ErrorResponException<>(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Exception Error",
                LocalDateTime.now(),
                ex.getMessage(),
                null
        );

        return new ResponseEntity<>(errorResponException, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<?> handleUsernameNotFoundException(UsernameNotFoundException ex) {
        ErrorResponException<?> errorResponException=new ErrorResponException<>(
                HttpStatus.NOT_FOUND.value(),
                "Exception Error",
                LocalDateTime.now(),
                ex.getMessage(),
                null
        );
        return new ResponseEntity<>(errorResponException, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<?> handleBadCredentialsException(BadCredentialsException ex) {
        ErrorResponException<?> errorResponException=new ErrorResponException<>(
                HttpStatus.FORBIDDEN.value(),
                "Exception Error",
                LocalDateTime.now(),
                ex.getMessage(),
                null
        );
        return new ResponseEntity<>(errorResponException, HttpStatus.FORBIDDEN);
    }
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleExitsEmail(IllegalArgumentException ex) {
        ErrorResponException<?> errorResponException=new ErrorResponException<>(
                HttpStatus.CONFLICT.value(),
                "Exception Error",
                LocalDateTime.now(),
                ex.getMessage(),
                null
        );
        return new ResponseEntity<>(errorResponException, HttpStatus.CONFLICT);

    }







}
