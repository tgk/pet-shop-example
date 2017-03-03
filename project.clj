(defproject pet-shop-example "0.1.0-SNAPSHOT"
  :description "An example of using clojure.spec for integration tests"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha10"]
                 [ring/ring-core "1.5.0"]
                 [cheshire "5.6.3"]
                 [compojure "1.5.1"]
                 [org.clojure/java.jdbc "0.7.0-alpha1"]
                 [org.postgresql/postgresql "9.3-1103-jdbc4"]
                 ;; might move to test portion
                 [org.clojure/test.check "0.9.0"]
                 [ring/ring-jetty-adapter "1.5.0"]
                 [clj-http "2.3.0"]])
