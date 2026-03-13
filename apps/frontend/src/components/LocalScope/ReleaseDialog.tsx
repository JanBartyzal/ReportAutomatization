/**
 * ReleaseDialog Component
 * Allows releasing local company data to the holding admin level
 */

import React, { useState } from 'react';
import {
    Dialog,
    DialogTrigger,
    DialogSurface,
    DialogTitle,
    DialogBody,
    DialogActions,
    DialogContent,
    Button,
    Spinner,
    makeStyles,
    tokens,
    Subtitle1,
    Body1,
} from '@fluentui/react-components';
import { ArrowUpload24Regular } from '@fluentui/react-icons';
import { reportBrand } from '../../theme/brandTokens';

const useStyles = makeStyles({
    warningBox: {
        padding: tokens.spacingHorizontalM,
        backgroundColor: tokens.colorStatusWarningBackground1,
        borderLeft: `4px solid ${tokens.colorStatusWarningBorder1}`,
        marginTop: tokens.spacingVerticalM,
    }
});

interface ReleaseDialogProps {
    dataId: string;
    dataName: string;
    period: string;
    trigger?: React.ReactElement;
}

export const ReleaseDialog: React.FC<ReleaseDialogProps> = ({ dataName, period, trigger }) => {
    const styles = useStyles();
    const [isOpen, setIsOpen] = useState(false);
    const [isReleasing, setIsReleasing] = useState(false);

    const handleRelease = async () => {
        setIsReleasing(true);
        // Simulate API call
        await new Promise(resolve => setTimeout(resolve, 1500));
        setIsReleasing(false);
        setIsOpen(false);
        alert(`Data for "${dataName}" (${period}) has been released to Holding Admin.`);
    };

    return (
        <Dialog open={isOpen} onOpenChange={(_, data) => setIsOpen(data.open)}>
            <DialogTrigger disableButtonEnhancement>
                {trigger || (
                    <Button icon={<ArrowUpload24Regular />} appearance="subtle">
                        Release
                    </Button>
                )}
            </DialogTrigger>
            <DialogSurface>
                <DialogBody>
                    <DialogTitle>Release Data to Holding</DialogTitle>
                    <DialogContent>
                        <Subtitle1>{dataName}</Subtitle1>
                        <Body1 block style={{ marginTop: tokens.spacingVerticalS }}>
                            Period: <strong>{period}</strong>
                        </Body1>
                        
                        <div className={styles.warningBox}>
                            <Body1 block>
                                <strong>Important:</strong> Once released, this data will be visible to Holding Administrators and can be pulled into central reports.
                            </Body1>
                        </div>
                        
                        <Body1 block style={{ marginTop: tokens.spacingVerticalM }}>
                            Are you sure you want to release this data?
                        </Body1>
                    </DialogContent>
                    <DialogActions>
                        <Button appearance="secondary" onClick={() => setIsOpen(false)} disabled={isReleasing}>
                            Cancel
                        </Button>
                        <Button 
                            appearance="primary" 
                            style={{ backgroundColor: reportBrand[90] }}
                            onClick={handleRelease}
                            icon={isReleasing ? <Spinner size="tiny" /> : undefined}
                            disabled={isReleasing}
                        >
                            {isReleasing ? 'Releasing...' : 'Release to Holding'}
                        </Button>
                    </DialogActions>
                </DialogBody>
            </DialogSurface>
        </Dialog>
    );
};

export default ReleaseDialog;
