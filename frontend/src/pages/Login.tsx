
import React, { useEffect } from 'react';
import { useMsal } from "@azure/msal-react";
import { loginRequest } from "../authConfig";
import { InteractionStatus } from "@azure/msal-browser";
import { Shield } from 'lucide-react';
import { Button } from '@fluentui/react-components';
import { useAuth } from '../components/auth/authContext';

export const Login: React.FC = () => {
    const { instance, inProgress } = useMsal();
    const { loginDev } = useAuth();


    // Pokud chceš auto-redirect, musíš kontrolovat status:
    useEffect(() => {
        // TOTO JE TA OPRAVA:
        // Nepouštěj login, pokud už se něco děje (Startup, HandleRedirect, atd.)
        if (inProgress === InteractionStatus.None) {
            // Volitelné: Odkomentuj, pokud chceš automatický redirect
            // instance.loginRedirect(loginRequest);
        }
    }, [inProgress, instance]);


    const handleLogin = async () => {
        // Explicitní kontrola
        if (inProgress !== InteractionStatus.None) {
            console.warn("Login blokován, status:", inProgress);
            return;
        }

        try {
            await instance.loginRedirect(loginRequest);
        } catch (e: any) {
            console.error("Login failed", e);
            // Pokud je to ta konkrétní chyba, zkusíme vyčistit cache (pro uživatele)
            if (e.errorCode === "interaction_in_progress") {
                alert("Došlo k chybě sezení. Stránka se obnoví.");
                sessionStorage.clear();
                window.location.reload();
            }
        }
    };

    return (
        <div className="min-h-screen bg-slate-900 flex items-center justify-center overflow-hidden relative">
            {/* Background Effects */}
            <div className="absolute top-0 left-0 w-full h-full overflow-hidden z-0">
                <div className="absolute -top-[30%] -left-[10%] w-[70%] h-[70%] rounded-full bg-blue-600/20 blur-[100px]" />
                <div className="absolute top-[20%] -right-[10%] w-[50%] h-[60%] rounded-full bg-cyan-500/10 blur-[100px]" />
            </div>

            <div className="w-full max-w-md p-8 relative z-10">
                <div className="bg-white/5 backdrop-blur-xl border border-white/10 rounded-2xl p-8 shadow-2xl">
                    <div className="text-center mb-8">
                        <div className="w-16 h-16 bg-blue-500 rounded-xl mx-auto flex items-center justify-center shadow-lg shadow-blue-500/40 mb-6">
                            <Shield className="w-8 h-8 text-white" />
                        </div>
                        <h1 className="text-3xl font-bold text-white mb-2">Welcome Back</h1>
                        <p className="text-slate-400">Sign in to access the Report Automation Platform</p>
                    </div>
                    {inProgress !== InteractionStatus.None ? (
                        <p>Načítám ověření...</p>
                    ) : (
                        <button
                            onClick={handleLogin}
                            className="w-full py-3.5 px-4 bg-blue-600 hover:bg-blue-500 text-white font-medium rounded-lg transition-all duration-200 shadow-lg shadow-blue-900/20 flex items-center justify-center space-x-2 group"
                        >
                            <svg className="w-5 h-5" viewBox="0 0 23 23" fill="none" xmlns="http://www.w3.org/2000/svg">
                                <path d="M11.5 0L0 0L0 11.5L11.5 11.5L11.5 0Z" fill="#F25022" />
                                <path d="M23 0L11.5 0L11.5 11.5L23 11.5L23 0Z" fill="#7FBA00" />
                                <path d="M11.5 11.5L0 11.5L0 23L11.5 23L11.5 11.5Z" fill="#00A4EF" />
                                <path d="M23 11.5L11.5 11.5L11.5 23L23 23L23 11.5Z" fill="#FFB900" />
                            </svg>

                            <span>Sign in with Microsoft Entra ID</span>
                        </button>

                    )}
                    {import.meta.env.DEV && (
                        <Button appearance="secondary" size="medium" onClick={() => loginDev()} style={{ width: '100%' }}>
                            Skip SSO (Dev Mode)
                        </Button>
                    )}

                    <div className="mt-8 pt-6 border-t border-white/10 text-center">
                        <p className="text-xs text-slate-500">
                            Authorized personnel only. All activities are monitored.
                        </p>
                    </div>
                </div>
            </div>
        </div>
    );
};
