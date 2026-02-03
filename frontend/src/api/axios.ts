import axios from 'axios';

// Vytvoření instance Axiosu
const api = axios.create({
    baseURL: import.meta.env.VITE_API_URL,
    withCredentials: true, // IMPORTANT: Send cookies with every request
    headers: {},
});

// Request Interceptor to add Authorization header
api.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('token');
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

// Response Interceptor for 401 handling
api.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response && error.response.status === 401) {
            // If backend says 401, it means cookie invalid/expired
            // Redirect to login or handled by AuthProvider check
            console.warn("API 401 Unauthorized - Session likely expired");

            // Optional: Force reload to trigger AuthProvider check or redirect
            // window.location.href = '/login'; 
        }
        return Promise.reject(error);
    }
);

export default api;