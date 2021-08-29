(defproject utilities "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/clojurescript "1.10.879"]
                 [integrant "0.8.0"]
                 [aero "1.1.6"]
                 [com.lucasbradstreet/cljs-uuid-utils "1.0.2"]
                 [metosin/malli "0.6.1"]
                 [com.github.seancorfield/next.jdbc "1.2.689"]
                 [org.postgresql/postgresql "42.2.23.jre7"]
                 [metosin/muuntaja "0.6.8"]
                 [camel-snake-kebab "0.4.2"]
                 [buddy/buddy-auth "3.0.1"]
                 [buddy/buddy-sign "3.4.1"]]
  :repl-options {:init-ns utilities.core})
