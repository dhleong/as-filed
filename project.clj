(defproject asfiled "0.1.0-SNAPSHOT"
  :description "Clearance Delivery made simple"
  :url "http://github.com/dhleong/asfiled"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [clj-time "0.10.0"]
                 [http-kit "2.1.18"]
                 [http-kit.fake "0.2.1"]]
  :main ^:skip-aot asfiled.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
