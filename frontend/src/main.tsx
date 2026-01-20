import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App'
// Fluent UI styly (volitelné, pokud je načítá Layout)
import { FluentProvider, webLightTheme } from '@fluentui/react-components'

ReactDOM.createRoot(document.getElementById('root')!).render(
    <React.StrictMode>
        <FluentProvider theme={webLightTheme}>
            <App />
        </FluentProvider>
    </React.StrictMode>,
)