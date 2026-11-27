import axios from "axios";

const API = axios.create({
    baseURL: import.meta.env.VITE_API_URL || "",
});

API.interceptors.response.use((response) => response, (error) => {
    if (error.response?.status === 401 && localStorage.getItem("token")) {
        localStorage.removeItem("token");
        localStorage.removeItem("role");
        window.dispatchEvent(new Event("bookverse:session-expired"));
    }
    return Promise.reject(error);
});

API.interceptors.request.use((config) => {

    const token = localStorage.getItem("token");

    if (token) config.headers.Authorization = `Bearer ${token}`;

    return config;
});

export default API;
