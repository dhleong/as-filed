(ns asfiled.skyvector-test
  (:use org.httpkit.fake)
  (:require [clojure.test :refer :all]
            [asfiled.skyvector :refer :all]
            [cheshire.core :refer [generate-string]]))

(def mock-result
  (generate-string
    {:plan {:points [{:name "LAGUARDIA" :th "281"}]}}))

(def mock-result-failure
  (generate-string
    {:plan {:points [{:name "LAGUARDIA"}]}}))

(def sop-exits-klga
  {:north [[290 360] [0 15]]
   :east [[16 95]]
   :south [[96 215]]
   :west [[216 289]]})

(deftest load-bearing-test
  (testing "Load degrees"
    (with-fake-http [#"dataLayer$" mock-result]
      (is (= 281 (load-bearing-to "klga" "kord")))))
  (testing "Gracefulness (eg VFR)"
    (with-fake-http [#"dataLayer$" mock-result-failure]
      (is (nil? (load-bearing-to "klga" "vfr"))))))

(deftest get-exit-test
  (testing "Get exit"
    (is (= :west (get-exit-to sop-exits-klga 281)))
    (is (= :north (get-exit-to sop-exits-klga 290)))
    (is (= :north (get-exit-to sop-exits-klga 360)))
    (is (= :north (get-exit-to sop-exits-klga 000)))
    (is (= :east (get-exit-to sop-exits-klga 50)))
    (is (= :east (get-exit-to sop-exits-klga 90)))
    (is (= :south (get-exit-to sop-exits-klga 180)))))
