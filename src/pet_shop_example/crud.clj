(ns pet-shop-example.crud
  (:require [compojure.core :refer :all]
            [cheshire.core :refer :all]
            [clojure.java.jdbc :refer :all])
  (:import [java.util UUID]))

(def db "postgresql://localhost/petshop")

(defroutes handler
  (GET "/stores/:store-id/animals" [store-id]
       {:status 200
        :body (generate-string
               (for [rel (query
                          db
                          ["SELECT name, cuddly, quantity
                            FROM animals_in_stocks
                            INNER JOIN animals
                            ON animals.id = animals_in_stocks.animal_id
                            WHERE store_id = ?"
                           (UUID/fromString store-id)])]
                 {:name (:name rel)
                  :cuddly (:cuddly rel)
                  :quantity (:quantity rel)}))}))
