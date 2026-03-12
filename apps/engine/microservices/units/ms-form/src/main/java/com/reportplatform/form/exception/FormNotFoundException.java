package com.reportplatform.form.exception;

import java.util.UUID;

public class FormNotFoundException extends RuntimeException {

    public FormNotFoundException(UUID formId) {
        super("Form not found: " + formId);
    }
}
