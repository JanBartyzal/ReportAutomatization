import '@testing-library/jest-dom/vitest';

// Mock MSAL (Microsoft Authentication Library)
vi.mock('@azure/msal-browser', () => ({
    PublicClientApplication: vi.fn().mockImplementation(() => ({
        initialize: vi.fn(),
        loginPopup: vi.fn(),
        acquireTokenSilent: vi.fn(),
        getAllAccounts: vi.fn().mockReturnValue([]),
        setActiveAccount: vi.fn(),
        getActiveAccount: vi.fn().mockReturnValue(null),
    })),
}));

vi.mock('@azure/msal-react', () => ({
    useMsal: () => ({
        instance: {
            getAllAccounts: vi.fn().mockReturnValue([]),
            getActiveAccount: vi.fn().mockReturnValue(null),
            acquireTokenSilent: vi.fn(),
        },
        accounts: [],
    }),
    AuthenticatedTemplate: ({ children }: { children: React.ReactNode }) => children,
    UnauthenticatedTemplate: ({ children }: { children: React.ReactNode }) => children,
    MsalProvider: ({ children }: { children: React.ReactNode }) => children,
}));

// Mock React Query
vi.mock('@tanstack/react-query', () => ({
    useQuery: vi.fn(),
    useMutation: vi.fn(),
    QueryClient: vi.fn().mockImplementation(() => ({
        setDefaultOptions: vi.fn(),
        clear: vi.fn(),
    })),
    QueryClientProvider: ({ children }: { children: React.ReactNode }) => children,
}));

// Mock window.matchMedia
Object.defineProperty(window, 'matchMedia', {
    writable: true,
    value: vi.fn().mockImplementation((query: string) => ({
        matches: false,
        media: query,
        onchange: null,
        addListener: vi.fn(),
        removeListener: vi.fn(),
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
        dispatchEvent: vi.fn(),
    })),
});

// Mock ResizeObserver
(globalThis as any).ResizeObserver = vi.fn().mockImplementation(() => ({
    observe: vi.fn(),
    unobserve: vi.fn(),
    disconnect: vi.fn(),
}));

// Mock scrollIntoView
Element.prototype.scrollIntoView = vi.fn();

// Mock URL.createObjectURL
URL.createObjectURL = vi.fn(() => 'blob:mock-url');
URL.revokeObjectURL = vi.fn();

// Suppress console errors in tests (optional - can be removed if needed during debugging)
const originalError = console.error;
beforeAll(() => {
    console.error = (...args: unknown[]) => {
        if (
            typeof args[0] === 'string' &&
            (args[0].includes('Warning:') || args[0].includes('act(...)'))
        ) {
            return;
        }
        originalError.call(console, ...args);
    };
});

afterAll(() => {
    console.error = originalError;
});
