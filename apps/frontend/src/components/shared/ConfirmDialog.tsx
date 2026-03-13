/**
 * ConfirmDialog — standardized confirmation modal per design system
 * Fluent Dialog, Level 4 shadow, radiusLg border radius
 * Danger variant (red CTA) for destructive actions
 */
import {
  Dialog,
  DialogSurface,
  DialogBody,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  makeStyles,
  tokens,
} from '@fluentui/react-components';
import { Warning24Regular } from '@fluentui/react-icons';

const useStyles = makeStyles({
  surface: {
    borderRadius: '12px',
    boxShadow: '0 8px 32px rgba(0, 0, 0, 0.16)',
    maxWidth: '480px',
  },
  dangerIcon: {
    color: tokens.colorStatusDangerForeground1,
    marginRight: '8px',
  },
  dangerButton: {
    backgroundColor: tokens.colorStatusDangerBackground3,
    color: tokens.colorNeutralForegroundOnBrand,
    ':hover': {
      backgroundColor: tokens.colorStatusDangerBackground3,
    },
  },
});

export interface ConfirmDialogProps {
  open: boolean;
  onClose: () => void;
  onConfirm: () => void;
  title: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  danger?: boolean;
  loading?: boolean;
}

export function ConfirmDialog({
  open,
  onClose,
  onConfirm,
  title,
  message,
  confirmLabel = 'Confirm',
  cancelLabel = 'Cancel',
  danger = false,
  loading = false,
}: ConfirmDialogProps) {
  const styles = useStyles();

  return (
    <Dialog open={open} onOpenChange={(_, data) => { if (!data.open) onClose(); }}>
      <DialogSurface className={styles.surface}>
        <DialogBody>
          <DialogTitle>
            {danger && <Warning24Regular className={styles.dangerIcon} />}
            {title}
          </DialogTitle>
          <DialogContent>{message}</DialogContent>
          <DialogActions>
            <Button appearance="secondary" onClick={onClose} disabled={loading}>
              {cancelLabel}
            </Button>
            <Button
              appearance="primary"
              onClick={onConfirm}
              disabled={loading}
              className={danger ? styles.dangerButton : undefined}
            >
              {loading ? 'Processing...' : confirmLabel}
            </Button>
          </DialogActions>
        </DialogBody>
      </DialogSurface>
    </Dialog>
  );
}
