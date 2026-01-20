import React from 'react';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import {
    FluentProvider,
    webLightTheme,
    makeStyles,
    tokens,
    TabList,
    Tab,
    Text,
    Avatar,
    Persona,
    Button
} from '@fluentui/react-components';
import {
    Board24Regular,
    History24Regular,
    Settings24Regular,
    SignOut24Regular,
    Cube24Regular
} from '@fluentui/react-icons';

import {
    Home24Regular,
    ArrowUpload24Regular,
    Code24Regular,
    ArrowSwap24Regular
} from '@fluentui/react-icons';

// --- STYLY (CSS-in-JS) ---
const useStyles = makeStyles({
    root: {
        display: 'flex',
        height: '100vh',
        backgroundColor: tokens.colorNeutralBackground2, // Jemně šedé pozadí pro celou app
    },
    sidebar: {
        width: '260px',
        backgroundColor: tokens.colorNeutralBackground1, // Bílá pro sidebar
        borderRight: `1px solid ${tokens.colorNeutralStroke1}`,
        display: 'flex',
        flexDirection: 'column',
        padding: '1rem',
        flexShrink: 0,
    },
    logoArea: {
        display: 'flex',
        alignItems: 'center',
        gap: '0.5rem',
        marginBottom: '2rem',
        paddingLeft: '0.5rem',
    },
    content: {
        flexGrow: 1,
        display: 'flex',
        flexDirection: 'column',
        overflow: 'hidden', // Aby scrolloval jen vnitřek, ne celá stránka
    },
    header: {
        height: '60px',
        backgroundColor: tokens.colorNeutralBackground1,
        borderBottom: `1px solid ${tokens.colorNeutralStroke1}`,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'flex-end', // User profil vpravo
        padding: '0 2rem',
    },
    mainScrollable: {
        flexGrow: 1,
        overflowY: 'auto', // Scrolluje jen obsah
        padding: '2rem',
    },
    bottomActions: {
        marginTop: 'auto',
        borderTop: `1px solid ${tokens.colorNeutralStroke1}`,
        paddingTop: '1rem',
    }
});

export const Layout: React.FC = () => {
    const styles = useStyles();
    const navigate = useNavigate();
    const location = useLocation();

    // Zjištění aktivního tabu na základě URL
    const selectedValue = location.pathname;

    return (
        <FluentProvider theme={webLightTheme} className={styles.root}>

            {/* --- LEVÝ SIDEBAR --- */}
            <nav className={styles.sidebar}>
                {/* Logo / Název */}
                <div className={styles.logoArea}>
                    <Cube24Regular primaryFill={tokens.colorBrandForeground1} />
                    <Text weight="bold" size={500}>Report Automatization</Text>
                </div>

                {/* Navigace */}
                <TabList
                    vertical
                    selectedValue={location.pathname}
                    onTabSelect={(_, data) => navigate(data.value as string)}
                >
                    <Tab value="/" icon={<Home24Regular />}>
                        Přehled
                    </Tab>

                    <div style={{ padding: '10px 10px 5px 10px', fontSize: '12px', color: '#666', fontWeight: 600 }}>
                        IMPORT
                    </div>

                    <Tab value="/import/opex" icon={<Code24Regular />}>
                        OPEX pptx
                    </Tab>
                </TabList>

                {/* Spodní část sidebaru (Logout) */}
                <div className={styles.bottomActions}>
                    <Button
                        appearance="subtle"
                        icon={<SignOut24Regular />}
                        onClick={() => alert("Logout logic here")}
                    >
                        Odhlásit se
                    </Button>
                </div>
            </nav>

            {/* --- PRAVÝ OBSAH --- */}
            <main className={styles.content}>

                {/* Horní lišta (Header) */}
                <header className={styles.header}>
                    {/* Placeholder pro uživatele (SaaS Tenant) */}
                    <Persona
                        name="Demo User"
                        secondaryText="Tenant: Acme Corp"
                        avatar={{ color: 'colorful' }}
                    />
                </header>

                {/* Dynamický obsah stránky (Dashboard, Detail...) */}
                <div className={styles.mainScrollable}>
                    <Outlet />
                </div>

            </main>
        </FluentProvider>
    );
};