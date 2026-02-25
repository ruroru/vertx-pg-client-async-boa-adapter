(ns jj.sql.boa.query.vertx-pg
  (:require [jj.sql.boa.async-query :as boa-query])
  (:import (io.vertx.core Handler)
           (io.vertx.sqlclient Row RowSet SqlClient Tuple)
           (io.vertx.sqlclient.desc ColumnDescriptor)
           (java.util.function Consumer)))

(defn- question-marks->positional
  [^String sql]
  (let [sb (StringBuilder.)
        idx (atom 0)]
    (loop [i 0]
      (if (>= i (.length sql))
        (.toString sb)
        (if (= (.charAt sql i) \?)
          (do (swap! idx inc)
              (.append sb (str "$" @idx))
              (recur (inc i)))
          (do (.append sb (.charAt sql i))
              (recur (inc i))))))))

(defn- rows->maps
  [^RowSet row-set]
  (let [col-names (mapv (fn [^ColumnDescriptor cd]
                          (keyword (.toLowerCase (.name cd))))
                        (.columnDescriptors row-set))]
    (loop [it (.iterator row-set)
           result (transient [])]
      (if (.hasNext it)
        (let [^Row row (.next it)
              m (persistent!
                  (reduce-kv (fn [acc i k]
                               (assoc! acc k (.getValue row (int i))))
                             (transient {})
                             col-names))]
          (recur it (conj! result m)))
        (persistent! result)))))

(defn- invoke-respond [respond data]
  (if (instance? Consumer respond)
    (.accept ^Consumer respond data)
    (respond data)))

(defrecord VertxPgAdapter []
  boa-query/AsyncBoaQuery
  (parameterless-query [_ client sql respond raise]
    (-> (.query ^SqlClient client sql)
        (.execute)
        (.onSuccess (reify Handler
                      (handle [_ row-set]
                        (invoke-respond respond (rows->maps row-set)))))
        (.onFailure (reify Handler
                      (handle [_ throwable]
                        (raise throwable))))))
  (query [_ client sql params respond raise]
    (let [pg-sql (question-marks->positional sql)
          tuple (Tuple/from ^"[Ljava.lang.Object;" (into-array Object params))]
      (-> (.preparedQuery ^SqlClient client pg-sql)
          (.execute tuple)
          (.onSuccess (reify Handler
                        (handle [_ row-set]
                          (invoke-respond respond (rows->maps row-set)))))
          (.onFailure (reify Handler
                        (handle [_ throwable]
                          (raise throwable))))))))

(defn ->VertxPgAdapter [] (VertxPgAdapter.))