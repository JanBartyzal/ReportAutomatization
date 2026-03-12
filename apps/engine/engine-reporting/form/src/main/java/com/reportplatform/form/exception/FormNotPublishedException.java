package com.reportplatform.form.exception;

import java.util.UUID;

public class FormNotPublishedException extends RuntimeException {

    public FormNotPublishedException(UUID formId) {
        super("Form is not published: " + formId);
    }
}
