import { useCallback, useContext, useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import API from "../axios";
import AppContext from "../Context/Context";

const blankAddress = { recipientName: "", phoneNumber: "", addressLine: "", locality: "", landmark: "", city: "", state: "", pinCode: "", country: "India", defaultAddress: false };
const paymentLabel = { CREATED: "Payment initiated", SUCCESS: "Payment successful", FAILED: "Payment failed" };
const orderLabel = { PENDING: "Pending", PAID: "Paid", CANCELLED: "Cancelled" };
const money = (amount, currency) => new Intl.NumberFormat("en-IN", { style: "currency", currency: currency || "INR" }).format(Number(amount || 0));

export default function Profile() {
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();
  const { logout } = useContext(AppContext);
  const [section, setSection] = useState(searchParams.get("section") || "personal");
  const [profile, setProfile] = useState(null);
  const [addresses, setAddresses] = useState([]);
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [form, setForm] = useState(blankAddress);
  const [editingId, setEditingId] = useState(null);
  const [saving, setSaving] = useState(false);

  const loadData = useCallback(async () => {
    setLoading(true); setError("");
    try {
      const [me, saved, history] = await Promise.all([API.get("/api/profile/me"), API.get("/api/addresses"), API.get("/api/orders/my-orders")]);
      setProfile(me.data); setAddresses(saved.data); setOrders(history.data);
    } catch { setError("Unable to load profile information. Please sign in again or retry."); }
    finally { setLoading(false); }
  }, []);
  useEffect(() => { loadData(); }, [loadData]);
  useEffect(() => { setSearchParams(section === "personal" ? {} : { section }, { replace: true }); }, [section, setSearchParams]);

  const startEdit = (address) => { setEditingId(address.id); setForm({ ...address }); setNotice(""); };
  const cancelEdit = () => { setEditingId(null); setForm(blankAddress); };
  const saveAddress = async (event) => {
    event.preventDefault(); setSaving(true); setNotice(""); setError("");
    try {
      const payload = { ...form, country: form.country || "India" };
      if (editingId) await API.put(`/api/addresses/${editingId}`, payload); else await API.post("/api/addresses", payload);
      cancelEdit(); await loadData(); setNotice("Address saved.");
    } catch (requestError) { setError(requestError.response?.data?.detail || "Could not save this address. Check the details and try again."); }
    finally { setSaving(false); }
  };
  const removeAddress = async (id) => {
    if (!window.confirm("Delete this saved address?")) return;
    try { await API.delete(`/api/addresses/${id}`); await loadData(); setNotice("Address deleted. Select another default if needed."); }
    catch { setError("Could not delete this address. Please try again."); }
  };
  const makeDefault = async (id) => {
    try { await API.patch(`/api/addresses/${id}/default`); await loadData(); setNotice("Default address updated."); }
    catch { setError("Could not update the default address."); }
  };
  const handleLogout = () => { logout(); navigate("/login"); };

  if (loading) return <main className="profile-page"><p>Loading your profile…</p></main>;
  return <main className="profile-page">
    <h1>My Profile</h1>
    {error && <div className="alert alert-danger" role="alert">{error}</div>}{notice && <div className="alert alert-success" role="status">{notice}</div>}
    <nav className="profile-tabs" aria-label="Profile sections">
      {[["personal", "Personal Information"], ["addresses", "Saved Addresses"], ["orders", "Order History"]].map(([key, label]) => <button key={key} className={section === key ? "active" : ""} onClick={() => setSection(key)}>{label}</button>)}
    </nav>
    {section === "personal" && <section className="profile-panel"><h2>Personal Information</h2><dl><dt>Name</dt><dd>{profile?.name || "—"}</dd><dt>Email</dt><dd>{profile?.email || "—"}</dd></dl><div className="profile-logout"><button className="btn btn-danger" onClick={handleLogout}>Logout</button></div></section>}
    {section === "addresses" && <section className="profile-panel"><h2>Saved Addresses</h2>
      <div className="address-list">{addresses.map((address) => <article className="saved-address" key={address.id}><div><strong>{address.recipientName}</strong> {address.defaultAddress && <span className="default-badge">Default</span>}<p>{address.addressLine}, {address.locality}{address.landmark ? `, near ${address.landmark}` : ""}, {address.city}, {address.state} {address.pinCode}, {address.country}</p><p>{address.phoneNumber}</p></div><div className="address-actions"><button className="btn btn-outline-primary btn-sm" onClick={() => startEdit(address)}>Edit</button>{!address.defaultAddress && <button className="btn btn-outline-success btn-sm" onClick={() => makeDefault(address.id)}>Set default</button>}<button className="btn btn-outline-danger btn-sm" onClick={() => removeAddress(address.id)}>Delete</button></div></article>)}</div>
      {addresses.length === 0 && <p>No saved addresses yet. Add one below to use it at checkout.</p>}
      <h3>{editingId ? "Edit address" : "Add an address"}</h3>
      <form className="address-form" onSubmit={saveAddress}>
        {[["recipientName", "Full name"], ["phoneNumber", "Phone number"], ["addressLine", "House / flat and street"], ["locality", "Area / locality"], ["landmark", "Landmark (optional)"], ["city", "City"], ["state", "State"], ["pinCode", "PIN code"], ["country", "Country"]].map(([key, label]) => <label key={key}>{label}<input value={form[key] || ""} required={key !== "landmark"} maxLength={key === "addressLine" ? 180 : 120} pattern={key === "phoneNumber" ? "[+]?[0-9\\s()-]{10,16}" : key === "pinCode" ? "[1-9][0-9]{5}" : undefined} title={key === "pinCode" ? "Enter a valid 6-digit Indian PIN code" : key === "phoneNumber" ? "Enter a valid phone number" : undefined} onChange={(event) => setForm({ ...form, [key]: event.target.value })} /></label>)}
        <label className="default-checkbox"><input type="checkbox" checked={form.defaultAddress} onChange={(event) => setForm({ ...form, defaultAddress: event.target.checked })} /> Set as default address</label>
        <div><button className="btn btn-primary" disabled={saving}>{saving ? "Saving…" : editingId ? "Save changes" : "Add address"}</button> {editingId && <button type="button" className="btn btn-secondary" onClick={cancelEdit}>Cancel</button>}</div>
      </form>
    </section>}
    {section === "orders" && <section className="profile-panel"><h2>Order History</h2>{orders.length === 0 ? <p>You haven’t placed any orders yet.</p> : orders.map((order) => <article className="order-card" key={order.orderId}>
      <header><div><strong>Order #{order.orderId}</strong><small>{order.createdAt ? new Date(order.createdAt).toLocaleString("en-IN") : "Date unavailable"}</small></div><span className={`status-pill status-${String(order.status || "").toLowerCase()}`}>{orderLabel[order.status] || order.status || "Status unavailable"}</span></header>
      <div className="order-items">{(order.items || []).map((item) => <div key={item.productId}><span>{item.productName || "Book"} × {item.quantity}</span><span>{money(Number(item.unitPrice) * Number(item.quantity), order.currency)}</span></div>)}</div>
      <p><strong>Total: {money(order.totalAmount, order.currency)}</strong>{order.paymentStatus && <span className="payment-status">Payment: {paymentLabel[order.paymentStatus] || order.paymentStatus}</span>}</p>
      <div className="delivery-snapshot"><strong>Delivery address</strong>{order.deliveryAddress ? <p>{order.deliveryAddress.recipientName}, {order.deliveryAddress.phoneNumber}<br />{order.deliveryAddress.addressLine}, {order.deliveryAddress.locality}{order.deliveryAddress.landmark ? `, near ${order.deliveryAddress.landmark}` : ""}, {order.deliveryAddress.city}, {order.deliveryAddress.state} {order.deliveryAddress.pinCode}, {order.deliveryAddress.country}</p> : <p>Address unavailable for this order.</p>}</div>
    </article>)}</section>}
  </main>;
}
