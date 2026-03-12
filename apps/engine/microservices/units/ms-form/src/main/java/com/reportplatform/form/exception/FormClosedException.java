package com.reportplatform.form.exception;

import java.util.UUID;

public class FormClosedException extends RuntimeException {

    public FormClosedException(UUID formId) {
        super("Form is closed and does not accept new submissions: " + formId);
    }
}
