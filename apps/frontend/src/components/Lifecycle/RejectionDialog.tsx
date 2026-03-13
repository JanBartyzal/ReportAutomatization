import React from 'react';
import {
    Dialog,
    // DialogTrigger,
    DialogSurface,
    DialogTitle,
    DialogBody,
    DialogActions,
    DialogContent,
    Button,
    Textarea,
    Body1,
    tokens,
} from '@fluentui/react-components';

interface RejectionDialogProps {
    open: boolean;
    onClose: () => void;
    onConfirm: () => void;
    comment: string;
    onCommentChange: (comment: string) => void;
    count: number;
}

/**
 * Rejection dialog with mandatory comment field
 */
export const RejectionDialog: React.FC<RejectionDialogProps> = ({
    open,
    onClose,
    onConfirm,
    comment,
    onCommentChange,
    count,
}) => {
    const isValid = comment.trim().length > 0;

    return (
        <Dialog open={open} onOpenChange={(_e, data) => !data.open && onClose()}>
            <DialogSurface>
                <DialogBody>
                    <DialogTitle>
                        {count > 1 ? `Reject ${count} Reports` : 'Reject Report'}
                    </DialogTitle>
                    <DialogContent>
                        <Body1 style={{ marginBottom: '16px' }}>
                            Please provide a reason for rejection. This comment will be visible to the report owner.
                        </Body1>
                        <Textarea
                            placeholder="Enter rejection reason..."
                            value={comment}
                            onChange={(_e, data) => onCommentChange(data.value)}
                            style={{ width: '100%', minHeight: '100px' }}
                        />
                    </DialogContent>
                    <DialogActions>
                        <Button appearance="secondary" onClick={onClose}>
                            Cancel
                        </Button>
                        <Button
                            appearance="primary"
                            onClick={onConfirm}
                            disabled={!isValid}
                            style={{ backgroundColor: tokens.colorPaletteRedBackground3, color: tokens.colorNeutralForegroundOnBrand }}
                        >
                            Reject
                        </Button>
                    </DialogActions>
                </DialogBody>
            </DialogSurface>
        </Dialog>
    );
};

export default RejectionDialog;
