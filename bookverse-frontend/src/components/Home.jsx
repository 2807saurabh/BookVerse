import React, { useContext, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import AppContext from "../Context/Context";
import unplugged from "../assets/unplugged.png"


const Home = ({ selectedCategory }) => {
  const { data, isError, addToCart, refreshData, cartError } = useContext(AppContext);
  const [pendingProduct, setPendingProduct] = useState(null);
  const [isDataFetched, setIsDataFetched] = useState(false);


  useEffect(() => {
    if (!isDataFetched) {
      refreshData();
      setIsDataFetched(true);
    }
  }, [refreshData, isDataFetched]);

  const filteredProducts = selectedCategory
    ? data.filter((product) => product.category === selectedCategory)
    : data;

  if (isError) {
    return (
      <h2 className="text-center" style={{ padding: "18rem" }}>
        <img src={unplugged} alt="Error" style={{ width: '100px', height: '100px' }} />
      </h2>
    );
  }
  return (
    <>
      {cartError && <p role="alert" className="text-center text-danger">{cartError}</p>}
      <div
        className="grid"
        style={{
          marginTop: "64px",
          display: "grid",
          gridTemplateColumns: "repeat(auto-fit, minmax(250px, 1fr))",
          gap: "20px",
          padding: "20px",
        }}
      >
        {filteredProducts.length === 0 ? (
          <h2
            className="text-center"
            style={{
              display: "flex",
              justifyContent: "center",
              alignItems: "center",
            }}
          >
            No Books Available
          </h2>
        ) : (
          filteredProducts.map((product) => {
            const { id, author, name, price, productAvailable, imageUrl } =
              product;
            const cardStyle = {
              width: "18rem",
              height: "12rem",
              boxShadow: "rgba(0, 0, 0, 0.24) 0px 2px 3px",
              backgroundColor: productAvailable ? "#fff" : "#ccc",
            };
            return (
              <div
                className="card mb-3"
                style={{
                  width: "250px",
                  height: "360px",
                  boxShadow: "0 4px 8px rgba(0,0,0,0.1)",
                  borderRadius: "10px",
                  overflow: "hidden",
                  backgroundColor: productAvailable ? "#fff" : "#ccc",
                  display: "flex",
                  flexDirection: "column",
                  justifyContent: 'flex-start',
                  alignItems: 'stretch'
                }}
                key={id}
              >
                <Link
                  to={`/product/${id}`}
                  style={{ textDecoration: "none", color: "inherit", flex: "1 1 auto" }}
                >
                  <img
                    src={`/api/product/${id}/image`}
                    alt={name}
                    style={{
                      width: "100%",
                      height: "150px",
                      objectFit: "cover",
                      padding: "5px",
                      margin: "0",
                      borderRadius: "10px 10px 10px 10px",
                    }}
                  />
                  <div
                    className="card-body"
                    style={{
                      flexGrow: 1,
                      display: "flex",
                      flexDirection: "column",
                      justifyContent: "space-between",
                      padding: "10px",
                    }}
                  >
                    <div>
                      <h5
                        className="card-title"
                        style={{ margin: "0 0 10px 0", fontSize: "1.2rem" }}
                      >
                        {name.toUpperCase()}
                      </h5>
                      <i
                        className="card-author"
                        style={{ fontStyle: "italic", fontSize: "0.8rem" }}
                      >
                        {"~ " + author}
                      </i>
                    </div>
                    <hr className="hr-line" style={{ margin: "10px 0" }} />
                    <div className="home-cart-price">
                      <h5
                        className="card-text"
                        style={{ fontWeight: "600", fontSize: "1.1rem", marginBottom: '5px' }}
                      >
                        <i class="bi bi-currency-rupee"></i>
                        {price}
                      </h5>
                    </div>
                  </div>
                </Link>
                <button
                  className="btn-hover color-9"
                  style={{ margin: "10px 25px" }}
                  onClick={async () => {
                    if (pendingProduct) return;
                    setPendingProduct(id);
                    try { await addToCart(product); } catch { /* context displays the error */ }
                    finally { setPendingProduct(null); }
                  }}
                  disabled={!productAvailable || pendingProduct === id}
                >
                  {!productAvailable ? "Out of Stock" : pendingProduct === id ? "Adding..." : "Add to Cart"}
                </button>
              </div>
            );
          })
        )}
      </div>
    </>
  );
};

export default Home;
