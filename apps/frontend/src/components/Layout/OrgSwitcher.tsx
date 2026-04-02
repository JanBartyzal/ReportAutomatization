import React from 'react';
import {
    Dropdown,
    Option,
    makeStyles,
    tokens,
    Spinner,
} from '@fluentui/react-components';
import { Building24Regular } from '@fluentui/react-icons';
import { useMe, useSwitchOrg } from '../../hooks/useAuth';
import { setDevOrgId } from '../../api/axios';

const useStyles = makeStyles({
    wrapper: {
        display: 'flex',
        alignItems: 'center',
        gap: tokens.spacingHorizontalS,
    },
    icon: {
        color: tokens.colorNeutralForeground3,
        flexShrink: 0,
    },
    dropdown: {
        minWidth: '180px',
        maxWidth: '280px',
    },
});

export const OrgSwitcher: React.FC = () => {
    const styles = useStyles();
    const { data: user, isLoading } = useMe();
    const switchOrg = useSwitchOrg();

    if (isLoading || !user?.organizations?.length) {
        return null;
    }

    // Single org — no need for switcher
    if (user.organizations.length <= 1) {
        return null;
    }

    const activeOrg = user.organizations.find((o) => o.id === user.active_org_id);
    const selectedValue = activeOrg?.name ?? user.organizations[0]?.name ?? '';

    const handleOrgChange = (_event: any, data: any) => {
        const selectedOrg = user.organizations.find((o) => o.name === data.optionValue);
        if (selectedOrg && selectedOrg.id !== user.active_org_id) {
            // Update immediately for dev bypass mode
            setDevOrgId(selectedOrg.id);
            // Call backend to persist the switch + invalidate all queries
            switchOrg.mutate(selectedOrg.id);
        }
    };

    return (
        <div className={styles.wrapper}>
            <Building24Regular className={styles.icon} />
            <Dropdown
                className={styles.dropdown}
                value={selectedValue}
                selectedOptions={[selectedValue]}
                onOptionSelect={handleOrgChange}
                disabled={switchOrg.isPending}
                size="small"
                appearance="underline"
            >
                {user.organizations.map((org) => (
                    <Option key={org.id} value={org.name}>
                        {org.name}
                    </Option>
                ))}
            </Dropdown>
            {switchOrg.isPending && <Spinner size="tiny" />}
        </div>
    );
};

export default OrgSwitcher;
