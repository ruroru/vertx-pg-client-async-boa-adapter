# pg-vertx-boa-adapter

## Installation 
Add adapter to dependency list

```clojure
[org.clojars.jj/vertx-pg-client-async-boa-adapter "1.0.1"]
```


## Usage

```clojure
(def data-source
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

(let [async-boa-fn (boa/build-async-query (vertx-adapter/->VertxPgAdapter) "select-all.sql")]
  (async-boa-fn data-source
                 (fn [result]
                   (println result))
                 (fn [err]
                   (println "error")))
  )

```

## License

Copyright © 2026 [ruroru](https://github.com/ruroru)

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
