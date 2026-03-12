package com.reportplatform.form.service;

import com.reportplatform.form.dto.ValidationResult;
import com.reportplatform.form.dto.ValidationResult.FieldError;
import com.reportplatform.form.model.FormFieldEntity;
import com.reportplatform.form.repository.FormFieldRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class ValidationService {

    private final FormFieldRepository formFieldRepository;

    public ValidationService(FormFieldRepository formFieldRepository) {
        this.formFieldRepository = formFieldRepository;
    }

    public ValidationResult validate(UUID formVersionId, Map<String, Object> data, boolean isSubmit) {
        var fields = formFieldRepository.findByFormVersionIdOrderBySortOrder(formVersionId);
        var errors = new ArrayList<FieldError>();
        var warnings = new ArrayList<FieldError>();

        for (var field : fields) {
            var value = data.get(field.getFieldKey());
            var fieldErrors = validateField(field, value);

            if (isSubmit) {
                errors.addAll(fieldErrors);
            } else {
                warnings.addAll(fieldErrors);
            }
        }

        // Cross-field validation
        for (var field : fields) {
            var crossErrors = validateCrossField(field, data, fields);
            if (isSubmit) {
                errors.addAll(crossErrors);
            } else {
                warnings.addAll(crossErrors);
            }
        }

        return ValidationResult.of(errors, warnings);
    }

    private List<FieldError> validateField(FormFieldEntity field, Object value) {
        var errors = new ArrayList<FieldError>();
        var props = field.getProperties();

        // Required check
        if (field.isRequired() && isBlank(value)) {
            errors.add(new FieldError(field.getFieldKey(), field.getLabel() + " is required", "required"));
            return errors;
        }

        if (isBlank(value)) {
            return errors;
        }

        String strValue = String.valueOf(value);

        // Type-specific validation
        switch (field.getFieldType()) {
            case "number", "percentage" -> {
                try {
                    var num = new BigDecimal(strValue);
                    if (props.containsKey("min")) {
                        var min = new BigDecimal(String.valueOf(props.get("min")));
                        if (num.compareTo(min) < 0) {
                            errors.add(new FieldError(field.getFieldKey(),
                                    field.getLabel() + " must be >= " + min, "min"));
                        }
                    }
                    if (props.containsKey("max")) {
                        var max = new BigDecimal(String.valueOf(props.get("max")));
                        if (num.compareTo(max) > 0) {
                            errors.add(new FieldError(field.getFieldKey(),
                                    field.getLabel() + " must be <= " + max, "max"));
                        }
                    }
                } catch (NumberFormatException e) {
                    errors.add(new FieldError(field.getFieldKey(),
                            field.getLabel() + " must be a valid number", "type"));
                }
            }
            case "text" -> {
                if (props.containsKey("regex")) {
                    String regex = String.valueOf(props.get("regex"));
                    if (!Pattern.matches(regex, strValue)) {
                        errors.add(new FieldError(field.getFieldKey(),
                                field.getLabel() + " does not match required format", "regex"));
                    }
                }
                if (props.containsKey("min")) {
                    int min = Integer.parseInt(String.valueOf(props.get("min")));
                    if (strValue.length() < min) {
                        errors.add(new FieldError(field.getFieldKey(),
                                field.getLabel() + " must be at least " + min + " characters", "min"));
                    }
                }
                if (props.containsKey("max")) {
                    int max = Integer.parseInt(String.valueOf(props.get("max")));
                    if (strValue.length() > max) {
                        errors.add(new FieldError(field.getFieldKey(),
                                field.getLabel() + " must be at most " + max + " characters", "max"));
                    }
                }
            }
            case "dropdown" -> {
                if (props.containsKey("options")) {
                    @SuppressWarnings("unchecked")
                    var options = (List<String>) props.get("options");
                    if (!options.contains(strValue)) {
                        errors.add(new FieldError(field.getFieldKey(),
                                field.getLabel() + " must be one of: " + options, "options"));
                    }
                }
            }
            default -> {
                // date, table, file_attachment - basic presence check only
            }
        }

        return errors;
    }

    private List<FieldError> validateCrossField(FormFieldEntity field, Map<String, Object> data,
                                                 List<FormFieldEntity> allFields) {
        var errors = new ArrayList<FieldError>();
        var props = field.getProperties();

        if (!props.containsKey("dependent_on")) {
            return errors;
        }

        @SuppressWarnings("unchecked")
        var dependency = (Map<String, Object>) props.get("dependent_on");
        String dependentFieldKey = String.valueOf(dependency.get("field"));
        String condition = String.valueOf(dependency.getOrDefault("condition", "not_empty"));
        Object conditionValue = dependency.get("value");

        Object dependentValue = data.get(dependentFieldKey);
        boolean dependencyMet = evaluateCondition(dependentValue, condition, conditionValue);

        if (dependencyMet && isBlank(data.get(field.getFieldKey()))) {
            errors.add(new FieldError(field.getFieldKey(),
                    field.getLabel() + " is required when " + dependentFieldKey + " " + condition,
                    "dependent_on"));
        }

        return errors;
    }

    private boolean evaluateCondition(Object value, String condition, Object conditionValue) {
        return switch (condition) {
            case "not_empty" -> !isBlank(value);
            case "equals" -> value != null && String.valueOf(value).equals(String.valueOf(conditionValue));
            case "greater_than" -> {
                if (value == null) yield false;
                try {
                    yield new BigDecimal(String.valueOf(value))
                            .compareTo(new BigDecimal(String.valueOf(conditionValue))) > 0;
                } catch (NumberFormatException e) {
                    yield false;
                }
            }
            default -> false;
        };
    }

    private boolean isBlank(Object value) {
        return value == null || String.valueOf(value).isBlank();
    }
}
