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
    setError("");

    try {
      const response = await API.post("/auth/login", {
        email,
        password,
      });
      console.log("========== LOGIN RESPONSE ==========");
console.log(response.data);
console.log("TOKEN:", response.data.token);
console.log("ROLE:", response.data.role);
console.log("====================================");

  console.log("LOGIN RESPONSE:", response.data);
login(response.data.token, response.data.role);
      navigate("/");
    } catch (err) {
      setError("Invalid email or password");
      console.error(err);
    }
  };
  

  return (
    <div className="login-page">
      <div className="login-card">

        <h2>Welcome Back</h2>
        <p className="login-subtitle">Login to your BookVerse account</p>

        <form onSubmit={handleLogin}>

          <div className="login-field">
            <label>Email</label>
            <input
              type="email"
              placeholder="Enter your email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
          </div>

          <div className="login-field">
            <label>Password</label>
            <input
              type="password"
              placeholder="Enter your password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          </div>

          <button className="login-btn" type="submit">
            Login
          </button>

        </form>

        {error && <p className="login-error">{error}</p>}

        <div className="login-divider">
          <span>OR</span>
        </div>

        <a
          className="google-login"
          href="/oauth2/authorization/google"
        >
          Login with Google
        </a>

        <p className="register-text">
          Don't have an account?
          <button
            type="button"
            className="create-account-btn"
            onClick={() => navigate("/register")}
          >
            Create Account
          </button>
        </p>

      </div>
    </div>
  );
}

export default Login;