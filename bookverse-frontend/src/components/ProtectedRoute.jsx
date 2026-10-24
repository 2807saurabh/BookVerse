import { Navigate } from "react-router-dom";
import { useContext } from "react";
import AppContext from "../Context/Context";

const ProtectedRoute = ({ children, adminOnly = false }) => {
  const token = localStorage.getItem("token");
  const { isAdmin } = useContext(AppContext);

  if (!token) {
    return <Navigate to="/login" replace />;
  }

  if (adminOnly && !isAdmin) {
    return <Navigate to="/" replace />;
  }

  return children;
};

export default ProtectedRoute;
