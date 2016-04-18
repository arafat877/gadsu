CREATE TABLE client (
  id CHAR(36) NOT NULL PRIMARY KEY,

  firstName VARCHAR(100) NOT NULL,
  lastName VARCHAR(100) NOT NULL,
  created TIMESTAMP NOT NULL,

  picture BLOB
);

CREATE TABLE treatment (
  id CHAR(36) NOT NULL PRIMARY KEY,
  id_client CHAR(36) NOT NULL,

  created TIMESTAMP NOT NULL,
  number INT NOT NULL,
  date TIMESTAMP NOT NULL, -- second and millisecond will be cut off by application
  note VARCHAR(1000) NOT NULL,

  FOREIGN KEY (id_client) REFERENCES client(id)
);
