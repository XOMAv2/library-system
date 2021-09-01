(defproject return-service "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [utilities "0.1.0-SNAPSHOT"]
                 [com.github.seancorfield/next.jdbc "1.2.689"]
                 [org.postgresql/postgresql "42.2.23.jre7"]
                 [metosin/reitit "0.5.15"]
                 [expound "0.8.9"]
                 [http-kit "2.5.3"]
                 [javax.servlet/servlet-api "2.5"]
                 [integrant "0.8.0"]
                 [buddy/buddy-auth "3.0.1"]
                 [buddy/buddy-hashers "1.8.1"]
                 [com.novemberain/langohr "5.2.0"]
                 [org.clojure/math.numeric-tower "0.0.4"]]
  :main ^:skip-aot service.return.system
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
