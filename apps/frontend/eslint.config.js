import js from '@eslint/js';
import tseslint from 'typescript-eslint';
import reactHooks from 'eslint-plugin-react-hooks';
import reactRefresh from 'eslint-plugin-react-refresh';
import globals from 'globals';
import importPlugin from 'eslint-plugin-import';

/**
 * Report Platform - ESLint Flat Config
 * Based on TypeScript and React best practices
 */
export default tseslint.config(
    // Ignore patterns
    {
        ignores: [
            'dist',
            'node_modules',
            '*.config.js',
            '*.config.ts',
            'coverage',
            '.vite',
        ],
    },

    // Base JavaScript rules
    js.configs.recommended,

    // TypeScript and TSX files
    ...tseslint.configs.recommended,

    // React refresh plugin for Vite HMR
    {
        plugins: {
            'react-hooks': reactHooks,
            'react-refresh': reactRefresh,
            'import': importPlugin,
        },
        rules: {
            ...reactHooks.configs.recommended.rules,
            'react-refresh/only-export-components': [
                'warn',
                { allowConstantExport: true },
            ],
        },
    },

    // Frontend-specific rules
    {
        languageOptions: {
            ecmaVersion: 2024,
            sourceType: 'module',
            globals: {
                ...globals.browser,
                ...globals.node,
            },
            parserOptions: {
                ecmaVersion: 'latest',
                ecmaFeatures: {
                    jsx: true,
                },
            },
        },
        files: ['**/*.{ts,tsx}'],
        rules: {
            // TypeScript rules
            '@typescript-eslint/no-unused-vars': [
                'warn',
                {
                    argsIgnorePattern: '^_',
                    varsIgnorePattern: '^_',
                },
            ],
            '@typescript-eslint/no-explicit-any': 'warn',
            '@typescript-eslint/explicit-function-return-type': 'off',
            '@typescript-eslint/explicit-module-boundary-types': 'off',
            '@typescript-eslint/no-empty-function': [
                'warn',
                {
                    allow: ['arrowFunctions', 'functions', 'methods'],
                },
            ],

            // React rules
            'react/react-in-jsx-scope': 'off',
            'react/prop-types': 'off',
            'react/display-name': 'off',

            // General rules
            'no-console': [
                'warn',
                {
                    allow: ['warn', 'error'],
                },
            ],
            'no-debugger': 'warn',
            'no-alert': 'warn',
            'no-undef': 'error',

            // Import rules
            'import/order': [
                'warn',
                {
                    groups: [
                        'builtin',
                        'external',
                        'internal',
                        'parent',
                        'sibling',
                        'index',
                    ],
                    'newlines-between': 'always',
                    alphabetize: {
                        order: 'asc',
                        caseInsensitive: true,
                    },
                },
            ],
            'import/no-unresolved': 'off', // Let TypeScript handle this

            // JSX rules
            'react/jsx-uses-react': 'off',
            'react/jsx-runtime': 'off',
        },
    },

    // Test files configuration
    {
        files: ['**/*.test.{ts,tsx}', '**/*.spec.{ts,tsx}'],
        languageOptions: {
            globals: {
                ...globals.jest,
                ...globals.node,
            },
        },
        rules: {
            '@typescript-eslint/no-explicit-any': 'off',
        },
    },

    // Configuration files
    {
        files: ['vite.config.ts', 'tsconfig.json', 'tsconfig.*.json'],
        rules: {
            '@typescript-eslint/no-namespace': 'off',
        },
    }
);
