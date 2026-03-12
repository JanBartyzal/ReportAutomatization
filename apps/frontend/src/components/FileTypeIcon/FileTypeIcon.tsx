import React, { ReactElement } from 'react';
import {
    DocumentPdf24Regular,
    DocumentTable24Regular,
    DocumentText24Regular,
    Slide24Regular,
} from '@fluentui/react-icons';

export type FileType = 'pdf' | 'xlsx' | 'csv' | 'pptx' | 'unknown';

interface FileTypeIconProps {
    mimeType: string;
    size?: number;
}

/**
 * Get file type from MIME type
 */
export function getFileType(mimeType: string): FileType {
    if (mimeType === 'application/pdf') return 'pdf';
    if (
        mimeType === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' ||
        mimeType === 'application/vnd.ms-excel'
    ) {
        return 'xlsx';
    }
    if (
        mimeType === 'text/csv' ||
        mimeType === 'application/csv' ||
        mimeType === 'text/comma-separated-values'
    ) {
        return 'csv';
    }
    if (
        mimeType === 'application/vnd.openxmlformats-officedocument.presentationml.presentation' ||
        mimeType === 'application/vnd.ms-powerpoint'
    ) {
        return 'pptx';
    }
    return 'unknown';
}

/**
 * FileTypeIcon component - displays appropriate icon based on MIME type
 * Uses Fluent UI icons per docs/UX-UI/03-figma-components.md
 */
export function FileTypeIcon({ mimeType, size = 24 }: FileTypeIconProps): ReactElement {
    const fileType = getFileType(mimeType);

    const iconProps = {
        width: size,
        height: size,
    };

    switch (fileType) {
        case 'pdf':
            return <DocumentPdf24Regular {...iconProps} style={{ color: 'var(--colorSemanticError)' }} />;
        case 'xlsx':
            return <DocumentTable24Regular {...iconProps} style={{ color: 'var(--chart-2)' }} />;
        case 'csv':
            return <DocumentText24Regular {...iconProps} style={{ color: 'var(--chart-3)' }} />;
        case 'pptx':
            return <Slide24Regular {...iconProps} style={{ color: 'var(--chart-4)' }} />;
        default:
            return <DocumentText24Regular {...iconProps} style={{ color: 'var(--colorNeutralForeground3)' }} />;
    }
}

/**
 * Get file extension from MIME type
 */
export function getFileExtension(mimeType: string): string {
    const typeMap: Record<string, string> = {
        'application/pdf': 'PDF',
        'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet': 'XLSX',
        'application/vnd.ms-excel': 'XLS',
        'text/csv': 'CSV',
        'application/csv': 'CSV',
        'text/comma-separated-values': 'CSV',
        'application/vnd.openxmlformats-officedocument.presentationml.presentation': 'PPTX',
        'application/vnd.ms-powerpoint': 'PPT',
    };
    return typeMap[mimeType] || 'FILE';
}
