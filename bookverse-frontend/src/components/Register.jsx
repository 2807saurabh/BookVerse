import { useState } from "react";
import { useNavigate } from "react-router-dom";
import API from "../axios";

function Register() {
  const navigate = useNavigate();

  const [form, setForm] = useState({
    name: "",
    email: "",
    password: "",
    confirmPassword: "",
  });

  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const handleChange = (e) => {
    setForm({
      ...form,
      [e.target.name]: e.target.value,
    });
  };

  const handleRegister = async (e) => {
    e.preventDefault();

    setError("");
    setSuccess("");

    if (
      !form.name ||
      !form.email ||
      !form.password ||
      !form.confirmPassword
    ) {
      setError("Please fill all fields");
      return;
    }

    if (form.password !== form.confirmPassword) {
      setError("Passwords do not match");
      return;
    }

    if (form.password.length < 6) {
      setError("Password must be at least 6 characters");
      return;
    }

    try {
      await API.post("/auth/register", {
        name: form.name,
        email: form.email,
        password: form.password,
      });

      setSuccess("Registration successful! Redirecting to login...");

      setTimeout(() => {
        navigate("/login");
      }, 1500);
    } catch (err) {
      console.error(err);

      if (err.response?.data) {
        setError(err.response.data);
      } else {
        setError("Registration failed. Please try again.");
      }
    }
  };

  return (
    <div className="register-page">
      <div className="register-card">

        <h2>Create Account</h2>

        <p className="register-subtitle">
          Create your BookVerse account
        </p>

        <form onSubmit={handleRegister}>

          <div className="register-field">
            <label>Full Name</label>

            <input
              type="text"
              name="name"
              placeholder="Enter your full name"
              value={form.name}
              onChange={handleChange}
              required
            />
          </div>

          <div className="register-field">
            <label>Email</label>

            <input
              type="email"
              name="email"
              placeholder="Enter your email"
              value={form.email}
              onChange={handleChange}
              required
            />
          </div>

          <div className="register-field">
            <label>Password</label>

            <input
              type="password"
              name="password"
              placeholder="Create a password"
              value={form.password}
              onChange={handleChange}
              required
            />
          </div>

          <div className="register-field">
            <label>Confirm Password</label>

            <input
              type="password"
              name="confirmPassword"
              placeholder="Confirm your password"
              value={form.confirmPassword}
              onChange={handleChange}
              required
            />
          </div>

          <button
            type="submit"
            className="register-btn"
          >
            Create Account
          </button>

        </form>

        {error && (
          <p className="register-error">
            {error}
          </p>
        )}

        {success && (
          <p className="register-success">
            {success}
          </p>
        )}

        <div className="register-divider">
          <span>Already have an account?</span>
        </div>

        <button
          type="button"
          className="go-login-btn"
          onClick={() => navigate("/login")}
        >
          Login
        </button>

      </div>
    </div>
  );
}

export default Register;