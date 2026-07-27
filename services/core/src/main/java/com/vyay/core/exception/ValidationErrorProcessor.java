package com.vyay.core.exception;


import com.vyay.core.dto.wrapper.ApiResponse;
import org.springframework.stereotype.Component;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Set;
import java.util.stream.Collectors;


@Component
public class ValidationErrorProcessor {
    public ApiResponse<Object>  processGlobalValidation(MethodArgumentNotValidException ex) {
        Set<String> missingParams = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .filter(err -> "NotBlank".equals(err.getCode()) || "NotNull".equals(err.getCode()))
                .map(FieldError::getField)
                .collect(Collectors.toSet());

        if (!missingParams.isEmpty()) {
            String msg = "Mandatory parameter(s) missing: " + String.join(", ", missingParams);
            System.out.println("missing error");
            return ApiResponse.error(msg, "ERR_MISSING_PARAM");
        }

        return null;
    }

}
