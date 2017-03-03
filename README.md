# pet-shop-example

This is a basic example of a Pet shop CRUD application along with
integration tests that utilise clojure.spec to generate data that is
then loaded into the SQL datastore.

## Usage

Please make sure that the Postgres database `petshop` is available on
`localhost` and that the database structure from `structure.sql` has
been loaded into the database. Once that is done the tests can be run by
issuing

    lein test

## License

Copyright © 2017 Thomas G. Kristensen

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
