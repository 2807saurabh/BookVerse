import { useContext, useState } from "react";
import { useNavigate } from "react-router-dom";
import API from "../axios";
import AppContext from "../Context/Context";

function Login() {
  const navigate = useNavigate();
  const { login } = useContext(AppContext);

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");

  const handleLogin = async (e) => {
    e.preventDefault();

    try {
      const response = await API.post("/auth/login", {
        email,
        password,
      });

      login(response.data.token);

      navigate("/");
    } catch (err) {
      setError("Invalid email or password");
      console.error(err);
    }
  };

  return (
    <div style={{ maxWidth: "400px", margin: "50px auto" }}>
      <h2>Login</h2>

      <form onSubmit={handleLogin}>
        <input
          type="email"
          placeholder="Email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
        />

        <br />
        <br />

        <input
          type="password"
          placeholder="Password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />

        <br />
        <br />

        <button type="submit">Login</button>
      </form>

      <br />

      <a href="http://localhost:8080/oauth2/authorization/google">
        Login with Google
      </a>

      <br />
      <br />

      {error && <p style={{ color: "red" }}>{error}</p>}
    </div>
  );
}

export default Login;