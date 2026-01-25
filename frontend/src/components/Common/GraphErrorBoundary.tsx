
import React, { Component, ErrorInfo, ReactNode } from 'react';
import { AlertTriangle, RefreshCcw } from 'lucide-react';

interface Props {
    children: ReactNode;
    fallback?: ReactNode;
}

interface State {
    hasError: boolean;
    error: Error | null;
}

export class GraphErrorBoundary extends Component<Props, State> {
    public state: State = {
        hasError: false,
        error: null,
    };

    public static getDerivedStateFromError(error: Error): State {
        return { hasError: true, error };
    }

    public componentDidCatch(error: Error, errorInfo: ErrorInfo) {
        console.error('Graph Error Boundary caught an error:', error, errorInfo);
    }

    public handleRetry = () => {
        this.setState({ hasError: false, error: null });
    };

    public render() {
        if (this.state.hasError) {
            if (this.props.fallback) {
                return this.props.fallback;
            }

            return (
                <div className="flex flex-col items-center justify-center p-8 bg-red-50/50 border border-red-100 rounded-xl h-full min-h-[300px]">
                    <div className="bg-red-100 p-3 rounded-full mb-4">
                        <AlertTriangle className="w-6 h-6 text-red-600" />
                    </div>
                    <h3 className="text-lg font-semibold text-red-900 mb-2">
                        Unable to render chart
                    </h3>
                    <p className="text-sm text-red-700 text-center max-w-xs mb-6">
                        {this.state.error?.message || "There was an unexpected error displaying this visualization."}
                    </p>
                    <button
                        onClick={this.handleRetry}
                        className="flex items-center space-x-2 px-4 py-2 bg-white border border-red-200 text-red-700 rounded-lg shadow-sm hover:bg-red-50 transition-colors text-sm font-medium"
                    >
                        <RefreshCcw className="w-4 h-4" />
                        <span>Try Again</span>
                    </button>
                </div>
            );
        }

        return this.props.children;
    }
}
