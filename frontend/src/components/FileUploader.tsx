import React, { useState, useRef, DragEvent, ChangeEvent } from 'react';
import {
    makeStyles,
    shorthands,
    tokens,
    Button,
    Text,
    ProgressBar,
    mergeClasses
} from '@fluentui/react-components';
import {
    CloudArrowUp48Regular,
    DocumentAdd24Regular
} from '@fluentui/react-icons';

// --- STYLY ---
const useStyles = makeStyles({
    root: {
        width: '100%',
        display: 'flex',
        flexDirection: 'column',
        gap: '1rem',
    },
    dropZone: {
        ...shorthands.border('2px', 'dashed', tokens.colorNeutralStroke1),
        borderRadius: tokens.borderRadiusMedium,
        backgroundColor: tokens.colorNeutralBackground1,
        padding: '3rem 2rem',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        gap: '1rem',
        transition: 'all 0.2s ease-in-out',
        cursor: 'pointer',
        ':hover': {
            ...shorthands.borderColor(tokens.colorBrandStroke1),
            backgroundColor: tokens.colorNeutralBackground1Hover,
        },
    },
    dropZoneActive: {
        ...shorthands.borderColor(tokens.colorBrandStroke1),
        backgroundColor: tokens.colorBrandBackground2, // Jemně modré pozadí při drag
        transform: 'scale(1.01)',
    },
    dropZoneDisabled: {
        opacity: 0.6,
        pointerEvents: 'none',
        cursor: 'not-allowed',
    },
    icon: {
        color: tokens.colorBrandForeground1,
    },
    textGroup: {
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        textAlign: 'center',
        gap: '0.25rem',
    }
});

// --- PROPS ---
interface FileUploaderProps {
    onUpload: (files: FileList) => void;
    isLoading?: boolean;
    accept?: string; // např. ".tf,.json,.bicep"
    multiple?: boolean;
}

export const FileUploader: React.FC<FileUploaderProps> = ({
    onUpload,
    isLoading = false,
    accept = ".pptx",
    multiple = false
}) => {
    const styles = useStyles();
    const inputRef = useRef<HTMLInputElement>(null);
    const [isDragOver, setIsDragOver] = useState(false);

    // --- HANDLERS ---

    // Kliknutí na tlačítko nebo zónu
    const handleClick = () => {
        if (!isLoading) {
            inputRef.current?.click();
        }
    };

    // Změna v inputu (výběr souborů)
    const handleInputChange = (e: ChangeEvent<HTMLInputElement>) => {
        if (e.target.files && e.target.files.length > 0) {
            onUpload(e.target.files);
        }
    };

    // Drag & Drop Events
    const handleDragOver = (e: DragEvent<HTMLDivElement>) => {
        e.preventDefault();
        e.stopPropagation();
        if (!isLoading) setIsDragOver(true);
    };

    const handleDragLeave = (e: DragEvent<HTMLDivElement>) => {
        e.preventDefault();
        e.stopPropagation();
        setIsDragOver(false);
    };

    const handleDrop = (e: DragEvent<HTMLDivElement>) => {
        e.preventDefault();
        e.stopPropagation();
        setIsDragOver(false);

        if (isLoading) return;

        if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
            onUpload(e.dataTransfer.files);
            // Vyčistit DataTransfer, aby se prohlížeč nesnažil soubor otevřít
            e.dataTransfer.clearData();
        }
    };

    return (
        <div className={styles.root}>
            {/* Skrytý Input */}
            <input
                ref={inputRef}
                type="file"
                multiple={multiple}
                accept={accept}
                style={{ display: 'none' }}
                onChange={handleInputChange}
            />

            {/* Drop Zone */}
            <div
                className={mergeClasses(
                    styles.dropZone,
                    isDragOver && styles.dropZoneActive,
                    isLoading && styles.dropZoneDisabled
                )}
                onClick={handleClick}
                onDragOver={handleDragOver}
                onDragLeave={handleDragLeave}
                onDrop={handleDrop}
                role="button"
                tabIndex={0}
                onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') handleClick(); }}
            >
                <CloudArrowUp48Regular className={styles.icon} />

                <div className={styles.textGroup}>
                    <Text weight="semibold" size={400}>
                        Klikněte pro výběr nebo přetáhněte soubory sem
                    </Text>

                </div>

                <Button
                    appearance="primary"
                    icon={<DocumentAdd24Regular />}
                    disabled={isLoading}
                >
                    Vybrat z počítače
                </Button>
            </div>

            {/* Loading State */}
            {isLoading && (
                <div>
                    <ProgressBar thickness="large" />
                    <Text align="center" style={{ display: 'block', marginTop: '0.5rem' }}>
                        Nahrávám a analyzuji infrastrukturu...
                    </Text>
                </div>
            )}
        </div>
    );
};