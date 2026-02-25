(ns pg-vertx-boa-adapter.core-test
  (:require [clojure.test :refer [deftest is]]
            [pg-embedded-clj.core :as pg]
            [jj.sql.async-boa :as boa]
            [jj.sql.boa.query.vertx-pg :as vertx-adapter]
            )
  (:import (io.vertx.core Vertx)
           (io.vertx.pgclient PgBuilder PgConnectOptions)))

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

(deftest no-params
  (pg/with-pg-fn {:port 54323}
                 (fn []
                   (let [result-promise (promise)
                         select-all-fn (boa/build-async-query (vertx-adapter/->VertxPgAdapter) "select-all.sql")]
                     (select-all-fn (create-pool)
                                    (fn [result]
                                      (println result)
                                      (deliver result-promise {:ok result}))
                                    (fn [err]
                                      (deliver result-promise {:err err})))
                     (let [result (deref result-promise 5000 :timeout)]
                       (is (not= result :timeout) "Query timed out")
                       (is (nil? (:err result)) (str "Query failed with error: " (:err result)))
                       (is (= [{:datname "postgres"} {:datname "template1"} {:datname "template0"}]
                              (:ok result))))))))


(deftest with-params
  (pg/with-pg-fn {:port 54323}
                 (fn []
                   (let [pool (create-pool)
                         create-table-fn (boa/build-async-query (vertx-adapter/->VertxPgAdapter) "create-table.sql")
                         insert-fn       (boa/build-async-query (vertx-adapter/->VertxPgAdapter) "insert.sql")
                         select-fn       (boa/build-async-query (vertx-adapter/->VertxPgAdapter) "select-users.sql")]

                     (let [p (promise)]
                       (create-table-fn pool
                                        (fn [_] (deliver p :ok))
                                        (fn [err] (deliver p {:err err})))
                       (let [r (deref p 5000 :timeout)]
                         (is (= :ok r) (str "Create table failed: " r))))

                     (doseq [[name email] [["Alice" "alice@example.com"]
                                           ["Bob"   "bob@example.com"]]]
                       (let [p (promise)]
                         (insert-fn pool
                                    {:name name
                                     :email email}
                                    (fn [_] (deliver p :ok))
                                    (fn [err] (deliver p {:err err})))
                         (let [r (deref p 5000 :timeout)]
                           (is (= :ok r) (str "Insert failed: " r)))))

                     (let [p (promise)]
                       (select-fn pool
                                  (fn [result] (deliver p {:ok result}))
                                  (fn [err]    (deliver p {:err err})))
                       (let [result (deref p 5000 :timeout)]
                         (println result )
                         (is (not= result :timeout) "Select timed out")
                         (is (nil? (:err result)) (str "Select failed: " (:err result)))
                         (is (= 2 (count (:ok result))))
                         (is (= "Alice" (:name (first (:ok result)))))
                         (is (= "Bob"   (:name (second (:ok result)))))))))))
