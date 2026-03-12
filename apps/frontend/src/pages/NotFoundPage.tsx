import { useNavigate } from 'react-router-dom';
import { Title1, Body1, Button, makeStyles } from '@fluentui/react-components';

const useStyles = makeStyles({
    container: {
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        minHeight: '100vh',
        padding: '24px',
    },
    title: {
        marginBottom: '8px',
    },
    subtitle: {
        marginBottom: '24px',
        color: '#666',
    },
});

export default function NotFoundPage() {
    const styles = useStyles();
    const navigate = useNavigate();

    return (
        <div className={styles.container}>
            <Title1 className={styles.title}>404</Title1>
            <Body1 className={styles.subtitle}>The page you're looking for doesn't exist.</Body1>
            <Button appearance="primary" onClick={() => navigate('/dashboard')}>
                Go to Dashboard
            </Button>
        </div>
    );
}
