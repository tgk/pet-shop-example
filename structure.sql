CREATE TABLE animals (
  id UUID,
  name VARCHAR,
  cuddly BOOLEAN
);

CREATE TABLE stores (
  id UUID,
  name VARCHAR,
  location VARCHAR
);

CREATE TABLE animals_in_stocks (
  animal_id UUID,
  store_id UUID,
  quantity INTEGER CONSTRAINT positive_quantity CHECK (quantity > 0)
);
