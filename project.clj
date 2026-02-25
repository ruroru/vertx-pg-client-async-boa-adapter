(defproject org.clojars.jj/vertx-pg-client-async-boa-adapter "1.0.1"
  :description "Async boa adapter for vertx pg client"
  :url "https://github.com/ruroru/vertx-pg-client-async-boa-adapter"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojars.jj/boa-async-query "1.0.6"]
                 [io.vertx/vertx-pg-client "5.0.8"]
                 [io.vertx/vertx-sql-client "5.0.8"]]
  :profiles {:test {:resource-paths ["test-resources"]
                    :dependencies   [
                                     [org.clojars.jj/async-boa-sql "1.0.6"]
                                     [org.clojars.bigsy/pg-embedded-clj "1.0.2"]
                                     ]}}

  :deploy-repositories [["clojars" {:url      "https://repo.clojars.org"
                                    :username :env/clojars_user
                                    :password :env/clojars_pass}]]

  :plugins [[org.clojars.jj/bump "1.0.4"]
            [org.clojars.jj/bump-md "1.1.0"]
            [org.clojars.jj/lein-git-tag "1.0.0"]
            [org.clojars.jj/strict-check "1.1.0"]]

  :repl-options {:init-ns pg-vertx-boa-adapter.core})
