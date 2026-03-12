import { Title2, Body1, makeStyles } from '@fluentui/react-components';

const useStyles = makeStyles({
    container: {
        padding: '24px',
    },
    title: {
        marginBottom: '24px',
    },
});

export default function SettingsPage() {
    const styles = useStyles();

    return (
        <div className={styles.container}>
            <Title2 className={styles.title}>Settings</Title2>
            <Body1>Settings page - Coming soon</Body1>
        </div>
    );
}
