import { useContext, useState } from "react";
import AppContext from "../Context/Context";
import CheckoutPopup from "./CheckoutPopup";
import { Button } from "react-bootstrap";
import API from "../axios";
import { useNavigate } from "react-router-dom";

function loadRazorpayScript() {
  return new Promise((resolve) => {
    if (window.Razorpay) return resolve(true);
    const script = document.createElement("script");
    script.src = "https://checkout.razorpay.com/v1/checkout.js";
    script.onload = () => resolve(true);
    script.onerror = () => resolve(false);
    document.body.appendChild(script);
  });
}

const Cart = () => {
  const { cart, cartLoading, cartError, setCartError, updateCartQuantity, removeFromCart, clearCart, refreshCart } = useContext(AppContext);
  const navigate = useNavigate();
  const [showModal, setShowModal] = useState(false);
  const [isProcessing, setIsProcessing] = useState(false);
  const [busyItem, setBusyItem] = useState(null);
  const [message, setMessage] = useState("");
  const [addresses, setAddresses] = useState([]);
  const [selectedAddressId, setSelectedAddressId] = useState("");
  const [addressLoading, setAddressLoading] = useState(false);
  const totalPrice = cart.reduce((total, item) => total + Number(item.subtotal ?? Number(item.price) * Number(item.quantity)), 0);

  const changeQuantity = async (item, delta) => {
    if (busyItem !== null || isProcessing) return;
    const quantity = Number(item.quantity) + delta;
    if (quantity < 1) return;
    if (quantity > Number(item.stockQuantity)) { setCartError("Cannot add more than available stock."); return; }
    setBusyItem(item.id);
    try { await updateCartQuantity(item.id, quantity); } catch { /* context provides an error */ }
    finally { setBusyItem(null); }
  };

  const removeItem = async (id) => {
    if (busyItem !== null || isProcessing) return;
    setBusyItem(id);
    try { await removeFromCart(id); } catch { /* context provides an error */ }
    finally { setBusyItem(null); }
  };

  const openCheckout = async () => {
    setShowModal(true); setAddressLoading(true); setMessage(""); setCartError("");
    try {
      const { data } = await API.get("/api/addresses");
      setAddresses(data);
      setSelectedAddressId((current) => current || String(data.find((address) => address.defaultAddress)?.id || data[0]?.id || ""));
    } catch (error) {
      setMessage(error.response?.data?.message || "Could not load your saved addresses. Please try again.");
    } finally { setAddressLoading(false); }
  };

  const handleCheckout = async () => {
    if (isProcessing) return;
    if (!selectedAddressId) { setMessage("Choose a delivery address before continuing."); return; }
    if (!cart.length) { setMessage("Your cart is empty."); return; }
    setIsProcessing(true); setMessage("");
    try {
      if (!(await loadRazorpayScript())) throw new Error("Unable to load Razorpay Checkout.");
      // The backend reads this user's saved cart and calculates the price from current database values.
      const { data: order } = await API.post("/api/payments/create-order", { addressId: Number(selectedAddressId) });
      let verificationStarted = false;
      const checkout = new window.Razorpay({
        key: order.keyId, amount: order.amount, currency: order.currency,
        name: "BookVerse", description: "Book purchase", order_id: order.razorpayOrderId,
        handler: async (paymentResponse) => {
          verificationStarted = true;
          try {
            await API.post("/api/payments/verify", {
              razorpayOrderId: paymentResponse.razorpay_order_id,
              razorpayPaymentId: paymentResponse.razorpay_payment_id,
              razorpaySignature: paymentResponse.razorpay_signature,
            });
            await refreshCart(); setShowModal(false); setMessage("Payment verified successfully.");
          } catch (error) {
            setMessage(error.response?.data?.message || "Payment was submitted, but verification is pending. Your cart has been preserved.");
          } finally { setIsProcessing(false); }
        },
        theme: { color: "#3399cc" },
        modal: { ondismiss: () => { if (!verificationStarted) { setIsProcessing(false); setMessage("Checkout closed. Your cart is unchanged; payment status may still be pending."); } } },
      });
      checkout.on("payment.failed", (response) => {
        setMessage(response.error?.description || "Payment failed. Your cart is unchanged.");
        setIsProcessing(false);
      });
      checkout.open();
    } catch (error) {
      setMessage(error.response?.data?.message || error.response?.data?.detail || error.message || "Unable to start checkout.");
      setIsProcessing(false);
    }
  };

  return <div className="cart-container">
    <div className="shopping-cart">
      <div className="title">Book Cart</div>
      {(cartError || message) && <div role="alert" className="text-danger p-2">{cartError || message}</div>}
      {cartLoading ? <p role="status" className="p-4">Loading your cart…</p> : cart.length === 0 ? (
        <div className="empty" style={{ textAlign: "left", padding: "2rem" }}><h4>Your cart is empty</h4><p>Browse the collection and add a book to get started.</p></div>
      ) : <>
        <ul className="list-unstyled">
          {cart.map((item) => <li key={item.id} className="cart-item">
            <div className="item" style={{ display: "flex", alignItems: "center", gap: "1rem", flexWrap: "wrap" }}>
              <img src={`/api/product/${item.id}/image`} alt={item.name} className="cart-item-image" onError={(e) => { e.currentTarget.style.display = "none"; }} />
              <div className="description"><span>{item.author}</span><strong>{item.name}</strong><span>₹{Number(item.price).toFixed(2)} each</span></div>
              <div className="quantity" aria-label={`Quantity for ${item.name}`}>
                <button type="button" disabled={busyItem !== null || isProcessing || item.quantity <= 1} onClick={() => changeQuantity(item, -1)} aria-label="Decrease quantity">−</button>
                <span aria-live="polite" style={{ padding: "0 .75rem" }}>{item.quantity}</span>
                <button type="button" disabled={busyItem !== null || isProcessing || item.quantity >= item.stockQuantity} onClick={() => changeQuantity(item, 1)} aria-label="Increase quantity">+</button>
              </div>
              <div className="total-price" style={{ textAlign: "center" }}>₹{Number(item.subtotal ?? item.price * item.quantity).toFixed(2)}</div>
              <button type="button" className="remove-btn" disabled={busyItem !== null || isProcessing} onClick={() => removeItem(item.id)}>Remove</button>
            </div>
          </li>)}
        </ul>
        <div className="total">Total: ₹{totalPrice.toFixed(2)}</div>
        <Button className="btn btn-primary" style={{ width: "100%" }} disabled={isProcessing || busyItem !== null} onClick={openCheckout}>Checkout</Button>
        <Button variant="link" className="mt-2" disabled={isProcessing || busyItem !== null} onClick={() => clearCart().catch(() => {})}>Clear cart</Button>
      </>}
    </div>
    <CheckoutPopup show={showModal} handleClose={() => { if (!isProcessing) setShowModal(false); }}
      cartItems={cart} totalPrice={totalPrice} handleCheckout={handleCheckout} isProcessing={isProcessing}
      addresses={addresses} selectedAddressId={selectedAddressId} setSelectedAddressId={setSelectedAddressId}
      addressLoading={addressLoading} onManageAddresses={() => navigate("/profile?section=addresses")} message={message} />
  </div>;
};

export default Cart;
