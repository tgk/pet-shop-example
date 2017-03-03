(ns integration-test
  (:require [clojure.test :refer :all]
            [clojure.spec :as s]
            [clojure.spec.gen :as gen]
            [clojure.set :refer [subset?]]
            [clojure.java.jdbc :as jdbc]
            [ring.adapter.jetty :as jetty]
            [clj-http.client :as client]
            [cheshire.core :refer :all]
            [pet-shop-example.crud :refer [handler]]))

;; specs for model objects

(s/def ::animal-id uuid?)

(s/def ::animal-name
  (s/and string? (fn [s] (> (count s) 0))))

(s/def ::cuddly boolean?)

(s/def ::animal
  (s/keys :req [::animal-id ::animal-name ::cuddly]))

(s/def ::animals
  (s/coll-of ::animal))


(s/def ::store-id uuid?)

(s/def ::store-name
  (s/and string? (fn [s] (> (count s) 0))))

(s/def ::location string?)

(s/def ::store
  (s/keys :req [::store-id ::store-name ::location]))

(s/def ::stores
  (s/coll-of ::store))


(s/def ::quantity pos-int?)

(s/def ::animals-in-stock
  (s/keys :req [::animal-id ::store-id ::quantity]))

(s/def ::animals-in-stocks
  (s/coll-of ::animals-in-stock))


;; generator for sane model of database

(defn animals-stock-consistency
  [database]
  (subset? (set (map ::animal-id (::animals-in-stocks database)))
           (set (map ::animal-id (::animals database)))))

(defn stores-stock-consistency
  [database]
  (subset? (set (map ::store-id (::animals-in-stocks database)))
           (set (map ::store-id (::stores database)))))

(s/def ::model
  (s/cat :animals (s/coll-of ::animal :min-count 3)
         :stores (s/coll-of ::store :min-count 3)
         :quantities (s/coll-of ::quantity :min-count 30)))

(defn cart-prod
  [xs ys]
  (for [x xs, y ys] [x y]))

(defn gen-database
  []
  (gen/fmap
   (fn [[animals stores quantities]]
     {::animals animals
      ::stores stores
      ::animals-in-stocks (for [[[animal store] quantity] (map vector (cart-prod animals stores) quantities)]
                            {::animal-id (::animal-id animal)
                             ::store-id (::store-id store)
                             ::quantity quantity})})
   (s/gen ::model)))

(s/def ::database
  (s/spec
   (s/and
    (s/keys :req [::animals ::stores ::animals-in-stocks])
    animals-stock-consistency
    stores-stock-consistency)
   :gen gen-database))


;; Prepare database for use

(def db "postgresql://localhost/petshop")

(defn truncate-tables!
  []
  (jdbc/execute! db "TRUNCATE animals;")
  (jdbc/execute! db "TRUNCATE stores;")
  (jdbc/execute! db "TRUNCATE animals_in_stocks;"))

(defn load-animals!
  [animals]
  (doseq [animal animals]
    (jdbc/insert!
     db :animals
     {:id (::animal-id animal)
      :name (::animal-name animal)
      :cuddly (::cuddly animal)})))

(defn load-stores!
  [stores]
  (doseq [store stores]
    (jdbc/insert!
     db :stores
     {:id (::store-id store)
      :name (::store-name store)
      :location (::location store)})))

(defn load-animals-in-stocks!
  [animals-in-stocks]
  (doseq [animals-in-stock animals-in-stocks]
    (jdbc/insert!
     db :animals_in_stocks
     {:animal_id (::animal-id animals-in-stock)
      :store_id (::store-id animals-in-stock)
      :quantity (::quantity animals-in-stock)})))

(defn prepare-sql-database!
  [database]
  (truncate-tables!)
  (load-animals! (::animals database))
  (load-stores! (::stores database))
  (load-animals-in-stocks! (::animals-in-stocks database)))


;; Testing the ring application

(defonce server nil)

(defn start-jetty! []
  (alter-var-root
   #'server
   (fn [_]
     (jetty/run-jetty
      handler
      {:port 8111 :join? false}))))

(defn stop-jetty! []
  (alter-var-root
   #'server
   (fn [server]
     (when server
       (.stop server)))))

;; TODO: some of this gritty code could be nicer with specter

(deftest animals-in-store-test
  (doseq [database (map first (s/exercise ::database 3))]
    (prepare-sql-database! database)
    (start-jetty!)
    (doseq [store (::stores database)]
      (let [expected-stocks (filter #(= (::store-id store) (::store-id %))
                                    (::animals-in-stocks database))
            response (client/get (str "http://localhost:8111/stores/" (::store-id store) "/animals"))
            json-body (parse-string (:body response))]
        (testing "The number of different animals in stock match"
          (is (= (count expected-stocks)
                 (count json-body))))
        (testing "The content of the returned animals"
          (is (= (set (for [stock expected-stocks]
                        (let [animal (first (filter #(= (::animal-id stock) (::animal-id %)) (::animals database)))]
                          {"name" (::animal-name animal)
                           "cuddly" (::cuddly animal)
                           "quantity" (::quantity stock)})))
                 (set json-body))))))
    (stop-jetty!)))
