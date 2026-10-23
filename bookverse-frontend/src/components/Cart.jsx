
import { useContext, useEffect, useState } from "react";
import AppContext from "../Context/Context";
import axios from "axios";
import CheckoutPopup from "./CheckoutPopup";
import { Button } from "react-bootstrap";
import API from "../axios";
import { useNavigate } from "react-router-dom";

const API_URL = "http://localhost:8080";

function loadRazorpayScript() {
  return new Promise((resolve) => {
    if (window.Razorpay) {
      resolve(true);
      return;
    }

    const script = document.createElement("script");
    script.src = "https://checkout.razorpay.com/v1/checkout.js";
    script.onload = () => resolve(true);
    script.onerror = () => resolve(false);
    document.body.appendChild(script);
  });
}

const Cart = () => {
  const { cart, removeFromCart, clearCart } = useContext(AppContext);
  const navigate = useNavigate();

  const [cartItems, setCartItems] = useState([]);
  const [showModal, setShowModal] = useState(false);
  const [isProcessing, setIsProcessing] = useState(false);
  const [message, setMessage] = useState("");
  const [addresses, setAddresses] = useState([]);
  const [selectedAddressId, setSelectedAddressId] = useState("");
  const [addressLoading, setAddressLoading] = useState(false);

  useEffect(() => {
    let cancelled = false;
    const imageUrls = [];

    async function fetchCartItems() {
      if (!cart || cart.length === 0) {
        setCartItems([]);
        return;
      }

      try {
        const response = await axios.get(`${API_URL}/api/product`);
        const products = response.data;

        const productMap = new Map(
          products.map((product) => [Number(product.id), product])
        );

        const validCart = cart
          .filter((item) => productMap.has(Number(item.id)))
          .map((item) => {
            const product = productMap.get(Number(item.id));

            return {
              ...product,
              quantity: Math.max(1, Number(item.quantity) || 1),
              imageUrl: null,
            };
          });

        const itemsWithImages = await Promise.all(
          validCart.map(async (item) => {
            try {
              const imageResponse = await axios.get(
                `${API_URL}/api/product/${item.id}/image`,
                { responseType: "blob" }
              );

              const imageUrl = URL.createObjectURL(imageResponse.data);
              imageUrls.push(imageUrl);

              return { ...item, imageUrl };
            } catch {
              return { ...item, imageUrl: "" };
            }
          })
        );

        if (!cancelled) {
          setCartItems(itemsWithImages);
        } else {
          imageUrls.forEach((url) => URL.revokeObjectURL(url));
        }
      } catch (error) {
        console.error("Unable to load cart:", error);
        if (!cancelled) {
          setMessage("Unable to load cart products. Please refresh.");
        }
      }
    }

    fetchCartItems();

    return () => {
      cancelled = true;
      imageUrls.forEach((url) => URL.revokeObjectURL(url));
    };
  }, [cart]);

  const totalPrice = cartItems.reduce(
    (total, item) =>
      total + Number(item.price) * Number(item.quantity),
    0
  );

  const handleIncreaseQuantity = (itemId) => {
    setCartItems((items) =>
      items.map((item) => {
        if (item.id !== itemId) return item;

        if (item.quantity >= item.stockQuantity) {
          setMessage("Cannot add more than available stock.");
          return item;
        }

        return { ...item, quantity: item.quantity + 1 };
      })
    );
  };

  const handleDecreaseQuantity = (itemId) => {
    setCartItems((items) =>
      items.map((item) =>
        item.id === itemId
          ? { ...item, quantity: Math.max(1, item.quantity - 1) }
          : item
      )
    );
  };

  const handleRemoveFromCart = (itemId) => {
    removeFromCart(itemId);
    setCartItems((items) => items.filter((item) => item.id !== itemId));
    setMessage("");
  };

  const handleCheckout = async () => {
    if (isProcessing) return;

    const token = localStorage.getItem("token");

    if (!token) {
      setMessage(
        "Please log in first. If you are already logged in, update getToken() to use your app's actual token storage key."
      );
      return;
    }

    if (cartItems.length === 0) {
      setMessage("Your cart is empty.");
      return;
    }

    if (!selectedAddressId) {
      setMessage("Choose a delivery address before continuing.");
      return;
    }

    setIsProcessing(true);
    setMessage("");

    try {
      const scriptLoaded = await loadRazorpayScript();

      if (!scriptLoaded) {
        throw new Error("Unable to load Razorpay Checkout.");
      }

      // The backend calculates the authoritative amount using DB prices.
      const orderResponse = await API.post(
        "/api/payments/create-order",
        {
          addressId: Number(selectedAddressId),
          items: cartItems.map((item) => ({
            productId: Number(item.id),
            quantity: Number(item.quantity),
          })),
        },
      );

      const order = orderResponse.data;

      const options = {
        key: order.keyId,
        amount: order.amount,
        currency: order.currency,
        name: "BookVerse",
        description: "Book purchase",
        order_id: order.razorpayOrderId,

        handler: async function (paymentResponse) {
          try {
            const verificationResponse = await API.post(
              "/api/payments/verify",
              {
                razorpayOrderId: paymentResponse.razorpay_order_id,
                razorpayPaymentId: paymentResponse.razorpay_payment_id,
                razorpaySignature: paymentResponse.razorpay_signature,
              },
            );

            // Only clear the cart after the backend confirms verification.
            clearCart();
            setCartItems([]);
            setShowModal(false);
            setMessage(
              verificationResponse.data ||
              "Payment verified successfully!"
            );
          } catch (error) {
            console.error(
              "Payment verification failed:",
              error.response?.data || error
            );

            setMessage(
              "Payment was submitted, but verification did not complete. Do not pay again immediately. Check the order/payment status before retrying."
            );
          } finally {
            setIsProcessing(false);
          }
        },

        prefill: {},
        theme: {
          color: "#3399cc",
        },
        modal: {
          ondismiss: () => {
            setIsProcessing(false);
            setMessage(
              "Checkout closed. Your order may still be pending payment confirmation."
            );
          },
        },
      };

      const checkout = new window.Razorpay(options);

      checkout.on("payment.failed", function (response) {
        console.error("Razorpay payment failed:", response.error);
        setMessage(
          response.error?.description || "Payment failed. You can try again."
        );
        setIsProcessing(false);
      });

      checkout.open();
    } catch (error) {
      console.error(
        "Checkout error:",
        error.response?.data || error
      );

      const backendMessage =
        error.response?.data?.detail ||
        error.response?.data?.message ||
        error.response?.data?.error;

      setMessage(
        backendMessage ||
        error.message ||
        "Unable to start checkout. Please try again."
      );
      setIsProcessing(false);
    }
  };

  const openCheckout = async () => {
    setShowModal(true);
    setAddressLoading(true);
    setMessage("");
    try {
      const { data } = await API.get("/api/addresses");
      setAddresses(data);
      setSelectedAddressId((current) => current || String(data.find((address) => address.defaultAddress)?.id || data[0]?.id || ""));
    } catch {
      setMessage("Could not load your saved addresses. Please sign in again or retry.");
    } finally {
      setAddressLoading(false);
    }
  };

  return (
    <div className="cart-container">
      <div className="shopping-cart">
        <div className="title">Book Cart</div>

        {message && (
          <div role="status" style={{ padding: "10px" }}>
            {message}
          </div>
        )}

        {cartItems.length === 0 ? (
          <div className="empty" style={{ textAlign: "left", padding: "2rem" }}>
            <h4>Your cart is empty</h4>
          </div>
        ) : (
          <>
            {cartItems.map((item) => (
              <li key={item.id} className="cart-item">
                <div
                  className="item"
                  style={{ display: "flex", alignItems: "center" }}
                >
                  <div>
                    {item.imageUrl ? (
                      <img
                        src={item.imageUrl}
                        alt={item.name}
                        className="cart-item-image"
                      />
                    ) : (
                      <div>No image</div>
                    )}
                  </div>

                  <div className="description">
                    <span>{item.author}</span>
                    <span>{item.name}</span>
                  </div>

                  <div className="quantity">
                    <button
                      type="button"
                      disabled={isProcessing}
                      onClick={() => handleIncreaseQuantity(item.id)}
                    >
                      +
                    </button>

                    <input
                      type="button"
                      value={item.quantity}
                      readOnly
                      aria-label="Quantity"
                    />

                    <button
                      type="button"
                      disabled={isProcessing}
                      onClick={() => handleDecreaseQuantity(item.id)}
                    >
                      -
                    </button>
                  </div>

                  <div className="total-price" style={{ textAlign: "center" }}>
                    ₹{(Number(item.price) * item.quantity).toFixed(2)}
                  </div>

                  <button
                    type="button"
                    className="remove-btn"
                    disabled={isProcessing}
                    onClick={() => handleRemoveFromCart(item.id)}
                  >
                    Remove
                  </button>
                </div>
              </li>
            ))}

            <div className="total">
              Total: ₹{totalPrice.toFixed(2)}
            </div>

            <Button
              className="btn btn-primary"
              style={{ width: "100%" }}
              disabled={isProcessing}
              onClick={openCheckout}
            >
              Checkout
            </Button>
          </>
        )}
      </div>

      <CheckoutPopup
        show={showModal}
        handleClose={() => {
          if (!isProcessing) setShowModal(false);
        }}
        cartItems={cartItems}
        totalPrice={totalPrice}
        handleCheckout={handleCheckout}
        isProcessing={isProcessing}
        addresses={addresses}
        selectedAddressId={selectedAddressId}
        setSelectedAddressId={setSelectedAddressId}
        addressLoading={addressLoading}
        onManageAddresses={() => navigate("/profile?section=addresses")}
        message={message}
      />
    </div>
  );
};

export default Cart;
