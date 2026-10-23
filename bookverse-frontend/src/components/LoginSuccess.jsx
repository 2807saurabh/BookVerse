import { useEffect, useContext } from "react";
import { useNavigate } from "react-router-dom";
import AppContext from "../Context/Context";

function LoginSuccess() {
    const navigate = useNavigate();
    const { login } = useContext(AppContext);

    useEffect(() => {
        const params = new URLSearchParams(window.location.search);

        const token = params.get("token");
        const role = params.get("role");

        console.log("GOOGLE LOGIN TOKEN:", token);
        console.log("GOOGLE LOGIN ROLE:", role);

        if (token) {
            login(token, role);
        }

        navigate("/");
    }, [navigate, login]);

    return <h2>Logging in...</h2>;
}

export default LoginSuccess;