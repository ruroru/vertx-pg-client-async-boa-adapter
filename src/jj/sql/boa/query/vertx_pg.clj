(ns jj.sql.boa.query.vertx-pg
  (:require [jj.sql.boa.protocol.query-builder :as query-builder]
            [jj.sql.boa.query :as boa-query]
            [jj.sql.boa.strategy.sequential :as sequential-strategy])
  (:import (io.vertx.sqlclient Row RowSet SqlClient Tuple)
           (io.vertx.sqlclient.desc ColumnDescriptor)
           (java.util.concurrent CompletableFuture)
           (java.util.function Function)))

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

(def ^:private ^Function rows->maps-fn
  (reify Function
    (apply [_ row-set]
      (rows->maps row-set))))

(def ^:private strategy (sequential-strategy/->SequentialStrategy))

(defrecord VertxPgAdapter []
  boa-query/BoaQuery
  (parameterless-query [_ client sql]
    (let [^CompletableFuture cf (-> (.query ^SqlClient client sql)
                                    (.execute)
                                    (.toCompletionStage)
                                    (.toCompletableFuture))]
      (.thenApply cf rows->maps-fn)))
  (query [_ client sql params]
    (let [tuple (Tuple/from ^"[Ljava.lang.Object;" (into-array Object params))
          ^CompletableFuture cf (-> (.preparedQuery ^SqlClient client sql)
                                    (.execute tuple)
                                    (.toCompletionStage)
                                    (.toCompletableFuture))]
      (.thenApply cf rows->maps-fn)))
  query-builder/QueryBuilder
  (build-query [_ tokens]
    (query-builder/build-query strategy tokens)))

(defn ->VertxPgAdapter [] (VertxPgAdapter.))
