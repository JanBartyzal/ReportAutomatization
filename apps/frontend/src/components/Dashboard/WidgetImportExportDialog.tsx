import {
    makeStyles,
    tokens,
    Button,
    Dialog,
    DialogSurface,
    DialogTitle,
    DialogBody,
    DialogContent,
    DialogActions,
    Body1,
} from '@fluentui/react-components';
import { ArrowDownload24Regular, ArrowUpload24Regular, Copy24Regular, Checkmark24Regular } from '@fluentui/react-icons';
import { useState } from 'react';
import type { WidgetConfig } from '@reportplatform/types';

const useStyles = makeStyles({
    content: {
        display: 'flex',
        flexDirection: 'column',
        gap: tokens.spacingVerticalL,
    },
    jsonArea: {
        width: '100%',
        minHeight: '200px',
        padding: tokens.spacingHorizontalM,
        fontFamily: 'monospace',
        fontSize: tokens.fontSizeBase200,
        border: `1px solid ${tokens.colorNeutralStroke1}`,
        borderRadius: tokens.borderRadiusMedium,
        backgroundColor: tokens.colorNeutralBackground2,
        resize: 'vertical',
        outline: 'none',
        color: tokens.colorNeutralForeground1,
    },
    buttonRow: {
        display: 'flex',
        gap: tokens.spacingHorizontalS,
    },
    success: {
        color: tokens.colorStatusSuccessForeground1,
        display: 'flex',
        alignItems: 'center',
        gap: tokens.spacingHorizontalXS,
    },
    error: {
        color: tokens.colorStatusDangerForeground1,
        padding: tokens.spacingHorizontalM,
        backgroundColor: tokens.colorStatusDangerBackground1,
        borderRadius: tokens.borderRadiusMedium,
    },
    note: {
        color: tokens.colorNeutralForeground2,
        fontSize: tokens.fontSizeBase200,
    },
});

interface WidgetExportDialogProps {
    open: boolean;
    onClose: () => void;
    widget: WidgetConfig | null;
}

export function WidgetExportDialog({ open, onClose, widget }: WidgetExportDialogProps) {
    const styles = useStyles();
    const [copied, setCopied] = useState(false);

    if (!widget) return null;

    const json = JSON.stringify(widget, null, 2);

    const handleCopy = async () => {
        await navigator.clipboard.writeText(json);
        setCopied(true);
        setTimeout(() => setCopied(false), 2000);
    };

    const handleDownload = () => {
        const blob = new Blob([json], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `widget-${widget.title.replace(/\s+/g, '_')}.json`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
    };

    return (
        <Dialog open={open} onOpenChange={(_ev, data) => !data.open && onClose()}>
            <DialogSurface>
                <DialogBody>
                    <DialogTitle>Export Widget</DialogTitle>
                    <DialogContent>
                        <div className={styles.content}>
                            <Body1>
                                Copy or download the widget configuration as JSON. You can import this later to restore the widget.
                            </Body1>
                            <textarea className={styles.jsonArea} readOnly value={json} />
                            <div className={styles.buttonRow}>
                                <Button
                                    appearance="primary"
                                    icon={copied ? <Checkmark24Regular /> : <Copy24Regular />}
                                    onClick={handleCopy}
                                >
                                    {copied ? 'Copied!' : 'Copy to Clipboard'}
                                </Button>
                                <Button
                                    appearance="subtle"
                                    icon={<ArrowDownload24Regular />}
                                    onClick={handleDownload}
                                >
                                    Download JSON
                                </Button>
                            </div>
                        </div>
                    </DialogContent>
                    <DialogActions>
                        <Button appearance="subtle" onClick={onClose}>Close</Button>
                    </DialogActions>
                </DialogBody>
            </DialogSurface>
        </Dialog>
    );
}

interface WidgetImportDialogProps {
    open: boolean;
    onClose: () => void;
    onImport: (widget: WidgetConfig) => void;
}

export function WidgetImportDialog({ open, onClose, onImport }: WidgetImportDialogProps) {
    const styles = useStyles();
    const [json, setJson] = useState('');
    const [error, setError] = useState<string | null>(null);
    const [parsedWidget, setParsedWidget] = useState<WidgetConfig | null>(null);

    const handleParse = () => {
        setError(null);
        setParsedWidget(null);

        if (!json.trim()) {
            setError('Please paste JSON content');
            return;
        }

        try {
            const parsed = JSON.parse(json) as WidgetConfig;

            if (!parsed.type || !parsed.title || !parsed.data_source || !parsed.config) {
                setError('Invalid widget structure. Required fields: type, title, data_source, config');
                return;
            }

            setParsedWidget(parsed);
        } catch {
            setError('Invalid JSON format');
        }
    };

    const handleImport = () => {
        if (parsedWidget) {
            onImport(parsedWidget);
            setJson('');
            setParsedWidget(null);
            setError(null);
            onClose();
        }
    };

    const handleClose = () => {
        setJson('');
        setParsedWidget(null);
        setError(null);
        onClose();
    };

    return (
        <Dialog open={open} onOpenChange={(_ev, data) => !data.open && handleClose()}>
            <DialogSurface>
                <DialogBody>
                    <DialogTitle>Import Widget</DialogTitle>
                    <DialogContent>
                        <div className={styles.content}>
                            <Body1>
                                Paste widget JSON configuration below to import it. You can get this JSON by exporting a widget first.
                            </Body1>
                            <textarea
                                className={styles.jsonArea}
                                value={json}
                                onChange={(e) => {
                                    setJson(e.target.value);
                                    setError(null);
                                    setParsedWidget(null);
                                }}
                                placeholder='{"type": "TABLE", "title": "My Widget", ...}'
                            />
                            {error && <div className={styles.error}><Body1>{error}</Body1></div>}
                            {parsedWidget && (
                                <div className={styles.success}>
                                    <Checkmark24Regular />
                                    <Body1>Widget parsed successfully: <strong>{parsedWidget.title}</strong></Body1>
                                </div>
                            )}
                            <div className={styles.buttonRow}>
                                <Button
                                    appearance="primary"
                                    onClick={handleParse}
                                    disabled={!json.trim()}
                                >
                                    Parse JSON
                                </Button>
                                <Button
                                    appearance="subtle"
                                    icon={<ArrowUpload24Regular />}
                                    onClick={handleImport}
                                    disabled={!parsedWidget}
                                >
                                    Import Widget
                                </Button>
                            </div>
                            <Body1 className={styles.note}>
                                Note: Imported widgets will be added to your dashboard with a new configuration.
                                You may need to adjust the data source settings.
                            </Body1>
                        </div>
                    </DialogContent>
                    <DialogActions>
                        <Button appearance="subtle" onClick={handleClose}>Cancel</Button>
                    </DialogActions>
                </DialogBody>
            </DialogSurface>
        </Dialog>
    );
}