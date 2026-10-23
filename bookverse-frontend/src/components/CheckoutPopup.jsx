
import { Modal, Button } from "react-bootstrap";

const CheckoutPopup = ({
  show,
  handleClose,
  cartItems,
  totalPrice,
  handleCheckout,
  isProcessing = false,
}) => {
  return (
    <Modal show={show} onHide={handleClose} centered>
      <Modal.Header closeButton={!isProcessing}>
        <Modal.Title>BookVerse Checkout</Modal.Title>
      </Modal.Header>

      <Modal.Body>
        {cartItems.map((item) => (
          <div
            key={item.id}
            className="checkout-item"
            style={{
              display: "flex",
              alignItems: "center",
              gap: "12px",
              marginBottom: "16px",
            }}
          >
            {item.imageUrl && (
              <img
                src={item.imageUrl}
                alt={item.name}
                className="cart-item-image"
                style={{ width: "90px", objectFit: "contain" }}
              />
            )}

            <div>
              <strong>{item.name}</strong>
              <p>Quantity: {item.quantity}</p>
              <p>
                Price: ₹
                {(Number(item.price) * item.quantity).toFixed(2)}
              </p>
            </div>
          </div>
        ))}

        <hr />

        <h5 style={{ textAlign: "right" }}>
          Total: ₹{totalPrice.toFixed(2)}
        </h5>

        <small>
          Your final amount is calculated and checked by the BookVerse backend.
        </small>
      </Modal.Body>

      <Modal.Footer>
        <Button
          variant="secondary"
          onClick={handleClose}
          disabled={isProcessing}
        >
          Close
        </Button>

        <Button
          variant="primary"
          onClick={handleCheckout}
          disabled={isProcessing || cartItems.length === 0}
        >
          {isProcessing ? "Processing..." : "Pay with Razorpay"}
        </Button>
      </Modal.Footer>
    </Modal>
  );
};

export default CheckoutPopup;
