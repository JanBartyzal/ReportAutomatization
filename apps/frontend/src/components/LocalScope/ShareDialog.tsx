/**
 * ShareDialog Component
 * Allows sharing local forms/templates with other organizations
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
    makeStyles,
    tokens,
    Subtitle1,
    Body1,
    Checkbox,
} from '@fluentui/react-components';
import { Share24Regular } from '@fluentui/react-icons';
import { reportBrand } from '../../theme/brandTokens';

const useStyles = makeStyles({
    orgList: {
        display: 'flex',
        flexDirection: 'column',
        gap: tokens.spacingVerticalS,
        maxHeight: '300px',
        overflowY: 'auto',
        padding: tokens.spacingHorizontalS,
        marginTop: tokens.spacingVerticalM,
    },
    orgItem: {
        display: 'flex',
        alignItems: 'center',
        gap: tokens.spacingHorizontalS,
    }
});

interface ShareDialogProps {
    itemId: string;
    itemName: string;
    itemType: 'FORM' | 'TEMPLATE';
    trigger?: React.ReactElement;
}

export const ShareDialog: React.FC<ShareDialogProps> = ({ itemId, itemName, itemType, trigger }) => {
    const styles = useStyles();
    const [isOpen, setIsOpen] = useState(false);
    const [selectedOrgs, setSelectedOrgs] = useState<string[]>([]);

    // Mock organizations
    const organizations = [
        { id: 'org-1', name: 'Acme Holding' },
        { id: 'org-2', name: 'Beta Solutions' },
        { id: 'org-3', name: 'Global Corp' },
        { id: 'org-4', name: 'Northwind Traders' },
    ];

    const handleShare = () => {
        // Logic to share the item
        alert(`Shared ${itemType} "${itemName}" with ${selectedOrgs.length} organizations.`);
        setIsOpen(false);
    };

    const toggleOrg = (orgId: string) => {
        setSelectedOrgs(prev => 
            prev.includes(orgId) ? prev.filter(id => id !== orgId) : [...prev, orgId]
        );
    };

    return (
        <Dialog open={isOpen} onOpenChange={(_, data) => setIsOpen(data.open)}>
            <DialogTrigger disableButtonEnhancement>
                {trigger || (
                    <Button icon={<Share24Regular />} appearance="subtle">
                        Share
                    </Button>
                )}
            </DialogTrigger>
            <DialogSurface>
                <DialogBody>
                    <DialogTitle>Share {itemType === 'FORM' ? 'Form' : 'Template'}</DialogTitle>
                    <DialogContent>
                        <Subtitle1>{itemName}</Subtitle1>
                        <Body1 block style={{ marginTop: tokens.spacingVerticalS }}>
                            Select organizations you want to share this {itemType.toLowerCase()} with:
                        </Body1>
                        
                        <div className={styles.orgList}>
                            {organizations.map(org => (
                                <div key={org.id} className={styles.orgItem}>
                                    <Checkbox 
                                        label={org.name} 
                                        checked={selectedOrgs.includes(org.id)}
                                        onChange={() => toggleOrg(org.id)}
                                    />
                                </div>
                            ))}
                        </div>
                    </DialogContent>
                    <DialogActions>
                        <Button appearance="secondary" onClick={() => setIsOpen(false)}>Cancel</Button>
                        <Button 
                            appearance="primary" 
                            style={{ backgroundColor: reportBrand[90] }}
                            onClick={handleShare}
                            disabled={selectedOrgs.length === 0}
                        >
                            Share Now
                        </Button>
                    </DialogActions>
                </DialogBody>
            </DialogSurface>
        </Dialog>
    );
};

export default ShareDialog;
