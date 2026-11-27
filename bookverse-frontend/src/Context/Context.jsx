import axios from "../axios";
import { useState, useEffect, createContext, useCallback, useRef } from "react";

const AppContext = createContext({});
const readGuestCart = () => {
  try { const cart = JSON.parse(localStorage.getItem("cart") || "[]"); return Array.isArray(cart) ? cart : []; }
  catch { return []; }
};
const toCartItems = (cart) => cart?.items || [];

export const AppProvider = ({ children }) => {
  const [data, setData] = useState([]);
  const [isError, setIsError] = useState("");
  const [cart, setCart] = useState(() => localStorage.getItem("token") ? [] : readGuestCart());
  const [cartLoading, setCartLoading] = useState(() => !!localStorage.getItem("token"));
  const [cartError, setCartError] = useState("");
  const [token, setToken] = useState(localStorage.getItem("token"));
  const [role, setRole] = useState(localStorage.getItem("role"));
  const [isLoggedIn, setIsLoggedIn] = useState(!!localStorage.getItem("token"));
  const skipNextCartFetch = useRef(false);
  const isAdmin = isLoggedIn && (role === "ROLE_ADMIN" || role === "ADMIN");

  const setServerCart = (response) => setCart(toCartItems(response));
  const refreshCart = useCallback(async () => {
    const requestToken = localStorage.getItem("token");
    if (!requestToken) return;
    setCartLoading(true);
    try { const response = await axios.get("/api/cart"); if (localStorage.getItem("token") === requestToken) { setServerCart(response.data); setCartError(""); } }
    catch (error) { setCartError(error.response?.data?.message || error.response?.data?.detail || "Could not load your cart. Please try again."); }
    finally { setCartLoading(false); }
  }, []);

  const addToCart = async (product, quantity = 1) => {
    setCartError("");
    if (localStorage.getItem("token")) {
      try { setServerCart((await axios.post("/api/cart/items", { productId: Number(product.id), quantity })).data); }
      catch (error) { const message = error.response?.data?.message || error.response?.data?.detail || "Could not add this book to your cart."; setCartError(message); throw error; }
      return;
    }
    const current = readGuestCart();
    const existing = current.find((item) => Number(item.id) === Number(product.id));
    const next = existing
      ? current.map((item) => Number(item.id) === Number(product.id) ? { ...item, quantity: Number(item.quantity || 1) + quantity } : item)
      : [...current, { ...product, quantity }];
    localStorage.setItem("cart", JSON.stringify(next)); setCart(next);
  };

  const updateCartQuantity = async (productId, quantity) => {
    setCartError("");
    if (localStorage.getItem("token")) {
      try { setServerCart((await axios.put(`/api/cart/items/${productId}`, { quantity })).data); }
      catch (error) { const message = error.response?.data?.message || error.response?.data?.detail || "Could not update the quantity."; setCartError(message); throw error; }
      return;
    }
    const next = readGuestCart().map((item) => Number(item.id) === Number(productId) ? { ...item, quantity } : item);
    localStorage.setItem("cart", JSON.stringify(next)); setCart(next);
  };

  const removeFromCart = async (productId) => {
    setCartError("");
    if (localStorage.getItem("token")) {
      try { setServerCart((await axios.delete(`/api/cart/items/${productId}`)).data); }
      catch (error) { const message = error.response?.data?.message || error.response?.data?.detail || "Could not remove this book."; setCartError(message); throw error; }
      return;
    }
    const next = readGuestCart().filter((item) => Number(item.id) !== Number(productId));
    localStorage.setItem("cart", JSON.stringify(next)); setCart(next);
  };

  const clearCart = async () => {
    setCartError("");
    if (localStorage.getItem("token")) {
      try { setServerCart((await axios.delete("/api/cart")).data); }
      catch (error) { const message = error.response?.data?.message || error.response?.data?.detail || "Could not clear your cart."; setCartError(message); throw error; }
    } else { localStorage.removeItem("cart"); setCart([]); }
  };

  const refreshData = useCallback(async () => {
    try { const response = await axios.get("/api/product"); setData(response.data); setIsError(""); }
    catch (error) { setIsError(error.response?.data?.message || error.message || "Could not load books."); }
  }, []);

  const login = useCallback(async (newToken, newRole) => {
    localStorage.setItem("token", newToken); localStorage.setItem("role", newRole || "ROLE_USER");
    skipNextCartFetch.current = true;
    setToken(newToken); setRole(newRole || "ROLE_USER"); setIsLoggedIn(true);
    const guestItems = readGuestCart();
    setCartLoading(true);
    try {
      let serverCart = (await axios.get("/api/cart")).data;
      for (const item of guestItems) {
        const productId = Number(item.id);
        const guestQuantity = Number(item.quantity) || 1;
        const originalQuantity = serverCart.items.find((line) => Number(line.id) === productId)?.quantity || 0;
        try { serverCart = (await axios.post("/api/cart/items", { productId, quantity: guestQuantity })).data; }
        catch (error) {
          // A lost response can follow a committed add. Re-read before deciding whether it is safe to retry.
          try {
            const latestCart = (await axios.get("/api/cart")).data;
            const latestQuantity = latestCart.items.find((line) => Number(line.id) === productId)?.quantity || 0;
            if (latestQuantity >= originalQuantity + guestQuantity) {
              serverCart = latestCart;
              const remaining = readGuestCart().filter((pending) => Number(pending.id) !== productId);
              if (remaining.length) localStorage.setItem("cart", JSON.stringify(remaining)); else localStorage.removeItem("cart");
              continue;
            }
            serverCart = latestCart;
          } catch { /* Leave this item saved as guest data when cart state is uncertain. */ }
          setCartError(error.response?.data?.message || "Some guest cart items could not be merged. Your guest cart is saved on this device.");
          setServerCart(serverCart); return;
        }
        const remaining = readGuestCart().filter((pending) => Number(pending.id) !== productId);
        if (remaining.length) localStorage.setItem("cart", JSON.stringify(remaining)); else localStorage.removeItem("cart");
      }
      setServerCart(serverCart); setCartError("");
    } catch (error) { setCartError(error.response?.data?.message || "Signed in, but your cart could not be synchronized."); }
    finally { setCartLoading(false); }
  }, []);

  const logout = () => {
    localStorage.removeItem("token"); localStorage.removeItem("role");
    setToken(null); setRole(null); setIsLoggedIn(false); setCart(readGuestCart()); setCartError("");
  };

  useEffect(() => { refreshData(); }, [refreshData]);
  useEffect(() => {
    if (!token) return;
    if (skipNextCartFetch.current) { skipNextCartFetch.current = false; return; }
    refreshCart();
  }, [token, refreshCart]);
  useEffect(() => {
    const expireSession = () => { setToken(null); setRole(null); setIsLoggedIn(false); setCart(readGuestCart()); setCartError("Your session expired. Please sign in again."); };
    window.addEventListener("bookverse:session-expired", expireSession);
    return () => window.removeEventListener("bookverse:session-expired", expireSession);
  }, []);

  return <AppContext.Provider value={{ data, isError, cart, cartLoading, cartError, setCartError,
    addToCart, updateCartQuantity, removeFromCart, refreshCart, refreshData, clearCart,
    token, isLoggedIn, isAdmin, role, login, logout }}>
    {children}
  </AppContext.Provider>;
};

export default AppContext;
