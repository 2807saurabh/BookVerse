import { useEffect } from "react";
import { useNavigate } from "react-router-dom";

function LoginSuccess() {

    const navigate = useNavigate();

    useEffect(() => {

        const token = new URLSearchParams(window.location.search).get("token");

        if (token) {
            localStorage.setItem("token", token);
        }

        navigate("/");

    }, [navigate]);

    return <h2>Logging in...</h2>;
}

export default LoginSuccess;