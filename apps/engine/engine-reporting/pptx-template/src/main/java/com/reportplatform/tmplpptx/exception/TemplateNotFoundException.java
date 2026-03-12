package com.reportplatform.tmplpptx.exception;

import java.util.UUID;

public class TemplateNotFoundException extends RuntimeException {

    public TemplateNotFoundException(UUID templateId) {
        super("PPTX template not found: " + templateId);
    }
}
