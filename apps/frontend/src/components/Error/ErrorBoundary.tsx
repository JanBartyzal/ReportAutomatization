/**
 * Error Boundary component for catching React rendering errors.
 *
 * Provides structured error reporting to OpenTelemetry and displays
 * user-friendly error UI with recovery options.
 *
 * Part of P5-W4-001: Frontend Error Tracking & Analytics
 */
import React, { Component, ReactNode, ErrorInfo } from 'react';
import {
    Card,
    Title3,
    Body1,
    Button,
    makeStyles,
    tokens,
} from '@fluentui/react-components';
import { Warning24Regular, ArrowReset24Regular, Home24Regular } from '@fluentui/react-icons';
import { reportError, getTraceId } from '../../telemetry/otel';
// import { useNavigate } from 'react-router-dom';

const useStyles = makeStyles({
    container: {
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        minHeight: '100vh',
        padding: tokens.spacingHorizontalL,
        backgroundColor: tokens.colorNeutralBackground2,
    },
    card: {
        maxWidth: '600px',
        width: '100%',
        padding: tokens.spacingVerticalXXL,
    },
    icon: {
        fontSize: '48px',
        color: tokens.colorPaletteRedForeground1,
        marginBottom: tokens.spacingVerticalM,
    },
    title: {
        marginBottom: tokens.spacingVerticalS,
        color: tokens.colorNeutralForeground1,
    },
    description: {
        marginBottom: tokens.spacingVerticalL,
        color: tokens.colorNeutralForeground2,
    },
    errorDetails: {
        backgroundColor: tokens.colorNeutralBackground3,
        padding: tokens.spacingVerticalM,
        borderRadius: tokens.borderRadiusMedium,
        marginBottom: tokens.spacingVerticalL,
        fontFamily: 'monospace',
        fontSize: tokens.fontSizeBase100,
        overflow: 'auto',
        maxHeight: '200px',
    },
    traceId: {
        marginTop: tokens.spacingVerticalS,
        color: tokens.colorNeutralForeground3,
        fontSize: tokens.fontSizeBase100,
    },
    actions: {
        display: 'flex',
        gap: tokens.spacingHorizontalM,
    },
});

interface Props {
    children: ReactNode;
    fallback?: ReactNode;
}

interface State {
    hasError: boolean;
    error: Error | null;
    errorInfo: ErrorInfo | null;
    traceId: string;
}

/**
 * Error Boundary that catches React component errors,
 * reports them to OpenTelemetry, and provides user-friendly UI.
 */
class ErrorBoundary extends Component<Props, State> {
    constructor(props: Props) {
        super(props);
        this.state = { hasError: false, error: null, errorInfo: null, traceId: '' };
    }

    static getDerivedStateFromError(error: Error): Partial<State> {
        return { hasError: true, error };
    }

    componentDidCatch(error: Error, errorInfo: ErrorInfo): void {
        this.setState({ errorInfo });

        // Report to OpenTelemetry
        reportError(error, {
            'error.component_stack': errorInfo.componentStack || '',
            'error.boundary': 'ReactErrorBoundary'
        });

        // Capture trace ID for display
        this.setState({ traceId: getTraceId() });

        // Also log to console for development
        console.error('[ErrorBoundary] Caught error:', error, errorInfo);
    }

    private handleReset = (): void => {
        this.setState({ hasError: false, error: null, errorInfo: null, traceId: '' });
    };

    private handleGoHome = (): void => {
        window.location.href = '/';
    };

    render(): ReactNode {
        if (this.state.hasError) {
            if (this.props.fallback) {
                return this.props.fallback;
            }

            return <ErrorFallback
                error={this.state.error}
                errorInfo={this.state.errorInfo}
                traceId={this.state.traceId}
                onReset={this.handleReset}
                onGoHome={this.handleGoHome}
            />;
        }

        return this.props.children;
    }
}

/**
 * Default error fallback UI.
 * Uses Fluent UI components per project design system.
 */
function ErrorFallback({
    error,
    errorInfo,
    traceId,
    onReset,
    onGoHome,
}: {
    error: Error | null;
    errorInfo: ErrorInfo | null;
    traceId: string;
    onReset: () => void;
    onGoHome: () => void;
}): React.ReactNode {
    const styles = useStyles();

    return (
        <div className={styles.container}>
            <Card className={styles.card}>
                <div className={styles.icon}>
                    <Warning24Regular />
                </div>

                <Title3 className={styles.title}>
                    Something went wrong
                </Title3>

                <Body1 className={styles.description}>
                    An unexpected error occurred. Please try again or return to the dashboard.
                </Body1>

                {error && (
                    <div className={styles.errorDetails}>
                        <strong>Error:</strong> {error.message}
                        {errorInfo?.componentStack && (
                            <>
                                <br /><br />
                                <strong>Component Stack:</strong>
                                <pre>{errorInfo.componentStack}</pre>
                            </>
                        )}
                        {traceId && (
                            <div className={styles.traceId}>
                                <strong>Trace ID:</strong> {traceId}
                            </div>
                        )}
                    </div>
                )}

                <div className={styles.actions}>
                    <Button
                        appearance="primary"
                        icon={<ArrowReset24Regular />}
                        onClick={onReset}
                    >
                        Try Again
                    </Button>
                    <Button
                        appearance="subtle"
                        icon={<Home24Regular />}
                        onClick={onGoHome}
                    >
                        Go to Dashboard
                    </Button>
                </div>
            </Card>
        </div>
    );
}

export default ErrorBoundary;
