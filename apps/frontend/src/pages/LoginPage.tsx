import { useEffect } from 'react';
import { useMsal } from '@azure/msal-react';
import { loginRequest } from '../auth/msalConfig';
import { Button, Title1, Body1, makeStyles, tokens } from '@fluentui/react-components';
import { isAuthBypassed } from '../auth/msalConfig';

/**
 * LoginPage styles per docs/UX-UI/02-design-system.md
 * - Using Fluent tokens throughout
 * - No hardcoded colors
 * - Card with proper elevation (Level 1 shadow)
 */
const useStyles = makeStyles({
    container: {
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        minHeight: '100vh',
        padding: tokens.spacingHorizontalL,
    },
    card: {
        maxWidth: '400px',
        width: '100%',
        padding: tokens.spacingHorizontalXXL,
        borderRadius: tokens.borderRadiusLarge,
        boxShadow: tokens.shadow4,
        backgroundColor: tokens.colorNeutralBackground1,
    },
    title: {
        marginBottom: tokens.spacingVerticalS,
        textAlign: 'center',
    },
    subtitle: {
        marginBottom: tokens.spacingHorizontalL,
        textAlign: 'center',
        color: tokens.colorNeutralForeground2,
    },
    button: {
        width: '100%',
        marginTop: tokens.spacingHorizontalM,
    },
});

export default function LoginPage() {
    const styles = useStyles();
    const { instance, inProgress } = useMsal();

    const handleLogin = () => {
        instance.loginPopup(loginRequest).catch((error) => {
            console.error('Login failed:', error);
        });
    };

    // If auth bypass is enabled, redirect to dashboard
    useEffect(() => {
        if (isAuthBypassed) {
            // In bypass mode, set a mock account
            instance.setActiveAccount({
                username: 'dev@localhost',
                localAccountId: 'dev-account',
                environment: 'localhost',
                tenantId: 'common',
                homeAccountId: 'dev-home-account',
                name: 'Dev User',
            });
        }
    }, [instance]);

    if (isAuthBypassed) {
        return (
            <div className={styles.container}>
                <div className={styles.card}>
                    <Title1 className={styles.title}>Development Mode</Title1>
                    <Body1 className={styles.subtitle}>
                        Auth bypass is enabled. Click below to continue.
                    </Body1>
                    <Button
                        className={styles.button}
                        appearance="primary"
                        onClick={() => window.location.href = '/dashboard'}
                    >
                        Continue to Dashboard
                    </Button>
                </div>
            </div>
        );
    }

    return (
        <div className={styles.container}>
            <div className={styles.card}>
                <Title1 className={styles.title}>Welcome</Title1>
                <Body1 className={styles.subtitle}>
                    Sign in with your Microsoft account to continue
                </Body1>
                <Button
                    className={styles.button}
                    appearance="primary"
                    onClick={handleLogin}
                    disabled={inProgress !== 'none'}
                >
                    {inProgress !== 'none' ? 'Signing in...' : 'Sign in with Microsoft'}
                </Button>
            </div>
        </div>
    );
}
