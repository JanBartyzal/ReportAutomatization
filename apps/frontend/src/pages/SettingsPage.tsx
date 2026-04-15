import {
    Title2,
    Title3,
    Body1,
    Body2,
    makeStyles,
    tokens,
    Card,
    CardHeader,
    RadioGroup,
    Radio,
    Divider,
} from '@fluentui/react-components';
import {
    WeatherSunnyRegular,
    WeatherMoonRegular,
    DesktopRegular,
} from '@fluentui/react-icons';
import { useAppTheme, type ThemeMode } from '../theme/ThemeProvider';

const useStyles = makeStyles({
    container: {
        padding: '32px',
        maxWidth: '720px',
    },
    pageTitle: {
        marginBottom: '32px',
        display: 'block',
    },
    section: {
        marginBottom: '24px',
    },
    card: {
        padding: '24px',
    },
    sectionTitle: {
        marginBottom: '4px',
        display: 'block',
    },
    sectionDesc: {
        color: tokens.colorNeutralForeground3,
        marginBottom: '20px',
        display: 'block',
    },
    radioOption: {
        display: 'flex',
        alignItems: 'center',
        gap: '8px',
    },
    radioIcon: {
        color: tokens.colorBrandForeground1,
        fontSize: '18px',
        flexShrink: 0,
    },
    radioLabel: {
        display: 'flex',
        flexDirection: 'column',
    },
    radioDesc: {
        color: tokens.colorNeutralForeground3,
        fontSize: tokens.fontSizeBase200,
    },
    divider: {
        marginBlock: '20px',
    },
});

interface ThemeOptionProps {
    icon: React.ReactNode;
    label: string;
    description: string;
}

function ThemeOptionLabel({ icon, label, description }: ThemeOptionProps) {
    const styles = useStyles();
    return (
        <span className={styles.radioOption}>
            <span className={styles.radioIcon}>{icon}</span>
            <span className={styles.radioLabel}>
                <Body1>{label}</Body1>
                <span className={styles.radioDesc}>{description}</span>
            </span>
        </span>
    );
}

export default function SettingsPage() {
    const styles = useStyles();
    const { mode, setMode } = useAppTheme();

    const handleThemeChange = (_: unknown, data: { value: string }) => {
        setMode(data.value as ThemeMode);
    };

    return (
        <div className={styles.container}>
            <Title2 className={styles.pageTitle}>Nastavení</Title2>

            <div className={styles.section}>
                <Card className={styles.card}>
                    <CardHeader
                        header={
                            <div>
                                <Title3 className={styles.sectionTitle}>Vzhled</Title3>
                                <Body2 className={styles.sectionDesc}>
                                    Zvolte barevný motiv aplikace. Možnost &quot;Systémový&quot; automaticky
                                    přepíná motiv podle nastavení vašeho operačního systému.
                                </Body2>
                            </div>
                        }
                    />
                    <Divider className={styles.divider} />
                    <RadioGroup
                        value={mode}
                        onChange={handleThemeChange}
                        layout="vertical"
                    >
                        <Radio
                            value="light"
                            label={
                                <ThemeOptionLabel
                                    icon={<WeatherSunnyRegular />}
                                    label="Světlý motiv"
                                    description="Světlé pozadí vhodné pro denní použití."
                                />
                            }
                        />
                        <Radio
                            value="dark"
                            label={
                                <ThemeOptionLabel
                                    icon={<WeatherMoonRegular />}
                                    label="Tmavý motiv"
                                    description="Tmavé pozadí snižující únavu očí při slabém osvětlení."
                                />
                            }
                        />
                        <Radio
                            value="system"
                            label={
                                <ThemeOptionLabel
                                    icon={<DesktopRegular />}
                                    label="Systémový"
                                    description="Automaticky sleduje nastavení motivu vašeho operačního systému."
                                />
                            }
                        />
                    </RadioGroup>
                </Card>
            </div>
        </div>
    );
}
