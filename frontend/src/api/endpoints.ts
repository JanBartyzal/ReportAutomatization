import axios from 'axios';
import {
    InfraPlan,
    PlanDetail,
    AnalysisResult,
    CompareResponse
} from '../..';

// Načtení URL z .env (nebo fallback pro dev)
const apiClient = axios.create({
    baseURL: import.meta.env.VITE_API_URL || '', // V dev mode Traefik přesměruje vše, co začíná /api
    headers: {
        'Content-Type': 'application/json',
    },
});

export const api = {
    parser: {
        // 1. Upload ZIP/TF souborů pro analýzu (Wizard Krok 1)
        analyzeSource: async (files: FileList, mode: 'terraform' | 'bicep') => {
            const formData = new FormData();
            Array.from(files).forEach((file) => formData.append('files', file));
            formData.append('mode', mode);

            const response = await apiClient.post<AnalysisResult>('/api/parser/analyze', formData, {
                headers: { 'Content-Type': 'multipart/form-data' },
            });
            return response.data;
        },

        // 2. Získání výsledku analýzy (pro refresh stránky)
        getAnalysisResult: async (sessionId: string) => {
            const response = await apiClient.get<AnalysisResult>(`/api/parser/analysis/${sessionId}`);
            return response.data;
        },

        // 3. Kompilace plánu s proměnnými (Wizard Krok 2)
        compilePlan: async (sessionId: string, variables: Record<string, string>) => {
            const response = await apiClient.post<{ id: string; status: string }>('/api/parser/compile', {
                session_id: sessionId,
                variables: variables
            });
            return response.data;
        },

        // Starší metody
        uploadPlan: async (files: FileList) => {
            const formData = new FormData();
            Array.from(files).forEach((file) => formData.append('files', file));
            const response = await apiClient.post<InfraPlan>('/api/parser/upload', formData, {
                headers: { 'Content-Type': 'multipart/form-data' },
            });
            return response.data;
        },

        getStatus: async (planId: string) => {
            const response = await apiClient.get<InfraPlan>(`/api/parser/plans/${planId}`);
            return response.data;
        },

        // Nová metoda pro dropdown v porovnávači
        listPlans: async () => {
            const response = await apiClient.get<InfraPlan[]>('/api/parser/plans');
            return response.data;
        }
    },

    pricing: {
        getPlanCosts: async (planId: string) => {
            const response = await apiClient.get<PlanDetail>(`/api/pricing/plans/${planId}/costs`);
            return response.data;
        },

        // --- ZDE BYLO CHYBĚJÍCÍ VOLÁNÍ ---
        comparePlans: async (planAId: string, planBId: string) => {
            // Posíláme ID jako query parametry: /api/pricing/compare?plan_a=1&plan_b=2
            const response = await apiClient.get<CompareResponse>('/api/pricing/compare', {
                params: {
                    plan_a: planAId,
                    plan_b: planBId
                }
            });
            return response.data;
        }
    },

    visualizer: {
        getDiagram: async (planId: string) => {
            const response = await apiClient.get<{ source: string }>(`/api/visualize/${planId}`);
            return response.data;
        }
    }
};