export type ValidationResult = {
    isValid: boolean;
    error?: string;
};

export type ValidationOptions = {
    maxSizeInBytes?: number; // e.g. 50 * 1024 * 1024 (50MB)
    allowedExtensions?: string[]; // e.g. ['.xlsx', '.pptx']
};

/**
 * Validates a file based on size and extension.
 * @param file The file object to validate.
 * @param options Validation constraints.
 * @returns ValidationResult object.
 */
export const validateFile = (file: File, options: ValidationOptions): ValidationResult => {
    const { maxSizeInBytes, allowedExtensions } = options;

    // 1. Check Size
    if (maxSizeInBytes && file.size > maxSizeInBytes) {
        const sizeInMB = (maxSizeInBytes / (1024 * 1024)).toFixed(2);
        return {
            isValid: false,
            error: `File is too large. Maximum size is ${sizeInMB} MB.`
        };
    }

    // 2. Check Extension
    if (allowedExtensions && allowedExtensions.length > 0) {
        const fileName = file.name.toLowerCase();
        const hasValidExtension = allowedExtensions.some(ext =>
            fileName.endsWith(ext.toLowerCase())
        );

        if (!hasValidExtension) {
            return {
                isValid: false,
                error: `Invalid file type. Allowed formats: ${allowedExtensions.join(", ")}`
            };
        }
    }

    return { isValid: true };
};
