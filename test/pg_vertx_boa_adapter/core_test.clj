(ns pg-vertx-boa-adapter.core-test
  (:require [clojure.test :refer [deftest is]]
            [pg-embedded-clj.core :as pg]
            [jj.sql.boa :as boa]
            [jj.sql.boa.query.vertx-pg :as vertx-adapter])
  (:import (io.vertx.core Vertx)
           (io.vertx.pgclient PgBuilder PgConnectOptions)
           (java.util.concurrent CompletableFuture)))

(defn create-pool []
  (let [connect-opts (-> (PgConnectOptions.)
                         (.setHost "localhost")
                         (.setPort 54323)
                         (.setDatabase "postgres")
                         (.setUser "postgres")
                         (.setPassword "postgres"))
        vertx (Vertx/vertx)]
    (-> (PgBuilder/pool)
        (.connectingTo connect-opts)
        (.using vertx)
        (.build))))

(deftest returns-completable-future
  (pg/with-pg-fn {:port 54323}
                 (fn []
                   (let [select-all-fn (boa/build-query (vertx-adapter/->VertxPgAdapter) "select-all.sql")
                         result (select-all-fn (create-pool))]
                     (is (instance? CompletableFuture result))
                     (is (= [{:datname "postgres"} {:datname "template1"} {:datname "template0"}]
                            (.get ^CompletableFuture result)))))))

(deftest no-params
  (pg/with-pg-fn {:port 54323}
                 (fn []
                   (let [select-all-fn (boa/build-query (vertx-adapter/->VertxPgAdapter) "select-all.sql")
                         future (select-all-fn (create-pool))]
                     (is (instance? CompletableFuture future))
                     (is (= [{:datname "postgres"} {:datname "template1"} {:datname "template0"}]
                            (.get ^CompletableFuture future)))))))

(deftest with-params
  (pg/with-pg-fn {:port 54323}
                 (fn []
                   (let [pool (create-pool)
                         create-table-fn (boa/build-query (vertx-adapter/->VertxPgAdapter) "create-table.sql")
                         insert-fn       (boa/build-query (vertx-adapter/->VertxPgAdapter) "insert.sql")
                         select-fn       (boa/build-query (vertx-adapter/->VertxPgAdapter) "select-users.sql")]

                     (let [create-future (create-table-fn pool)]
                       (is (instance? CompletableFuture create-future))
                       (.get ^CompletableFuture create-future))

                     (doseq [[name email] [["Alice" "alice@example.com"]
                                           ["Bob"   "bob@example.com"]]]
                       (let [insert-future (insert-fn pool [name email])]
                         (is (instance? CompletableFuture insert-future))
                         (.get ^CompletableFuture insert-future)))

                     (let [select-future (select-fn pool)]
                       (is (instance? CompletableFuture select-future))
                       (let [result (.get ^CompletableFuture select-future)]
                         (is (= 2 (count result)))
                         (is (= "Alice" (:name (first result))))
                         (is (= "Bob"   (:name (second result))))))))))
