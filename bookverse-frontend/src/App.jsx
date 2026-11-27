import "./App.css";
import { useState } from "react";
import Home from "./components/Home";
import Navbar from "./components/Navbar";
import Cart from "./components/Cart";
import AddProduct from "./components/AddProduct";
import Product from "./components/Product";
import { BrowserRouter, Routes, Route } from "react-router-dom";
import UpdateProduct from "./components/UpdateProduct";
import "bootstrap/dist/css/bootstrap.min.css";
import "bootstrap/dist/js/bootstrap.bundle.min.js";
import LoginSuccess from "./components/LoginSuccess";
import Login from "./components/Login";
import ProtectedRoute from "./components/ProtectedRoute";
import Register from "./components/Register";
import Profile from "./components/Profile";

function App() {
  const [selectedCategory, setSelectedCategory] = useState("");

  const handleCategorySelect = (category) => {
    setSelectedCategory(category);
    console.log("Selected category:", category);
  };
  return (
  <BrowserRouter>
    <Navbar onSelectCategory={handleCategorySelect} />

    <Routes>
      <Route
        path="/"
        element={
          <Home selectedCategory={selectedCategory} />
        }
      />

      <Route
        path="/add_product"
        element={
          <ProtectedRoute adminOnly>
            <AddProduct />
          </ProtectedRoute>
        }
      />

      <Route path="/product" element={<Product />} />
      <Route path="/product/:id" element={<Product />} />

      <Route
        path="/product/update/:id"
        element={
          <ProtectedRoute adminOnly>
            <UpdateProduct />
          </ProtectedRoute>
        }
      />

      <Route path="/login-success" element={<LoginSuccess />} />
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route path="/profile" element={<ProtectedRoute><Profile /></ProtectedRoute>} />

      <Route
        path="/cart"
        element={
          <ProtectedRoute>
            <Cart />
          </ProtectedRoute>
        }
      />
    </Routes>
  </BrowserRouter>
);
}

export default App;
