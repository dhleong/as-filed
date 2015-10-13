(ns asfiled.skyvector-test
  (:use org.httpkit.fake)
  (:require [clojure.test :refer :all]
            [asfiled.skyvector :refer :all]
            [cheshire.core :refer [generate-string]]))

(def mock-result
  (generate-string
    {:route [{:name "LAGUARDIA" :mh "293" :magvar "12.0"}]}))

(def mock-result-failure
  (generate-string
    {:route [{:name "LAGUARDIA"}]}))

(def mock-vor-result
  (generate-string
    {:route [{:name "LAGUARDIA"} {:n "BRIDGEPORT" :t "V"}]}))

(def sop-exits-klga
  {:north [[290 360] [0 15]]
   :east [[16 95]]
   :south [[96 215]]
   :west [[216 289]]})

(deftest load-bearing-test
  (testing "Load degrees"
    (with-fake-http [#"fpl" mock-result]
      (is (= 281 (load-bearing-to "klga" "kord")))))
  (testing "Gracefulness (eg VFR)"
    (with-fake-http [#"fpl" mock-result-failure]
      (is (nil? (load-bearing-to "klga" "vfr"))))))

(deftest load-vor-test
  (testing "Load VOR"
    (with-fake-http [#"fpl" mock-vor-result]
      (let [loaded (load-vor "klga" "bdr")]
        (is (= "BRIDGEPORT" (:name loaded)))
        (is (= "V" (:t loaded)))))))

(deftest cached-calls-test
  (testing "Get VOR"
    (clear-cache)
    (with-fake-http [#"fpl" "{}"]
      (is (nil? (get-vor "klga" "bdr"))))
    (with-fake-http [#"fpl" mock-vor-result]
      (let [loaded (get-vor "klga" "bdr")]
        (is (= "BRIDGEPORT" (:name loaded)))
        (is (= "V" (:t loaded)))))
    (with-fake-http [#"fpl" "{}"]
      (let [loaded (get-vor "klga" "bdr")]
        ;; should return the last, cached value
        (is (= "BRIDGEPORT" (:name loaded)))
        (is (= "V" (:t loaded)))))
    (clear-cache))
  (testing "Get Bearing"
    (clear-cache)
    (with-fake-http [#"fpl" "{}"]
      (is (nil? (get-bearing-to "klga" "kord"))))
    (with-fake-http [#"fpl" mock-result]
      (is (= 281 (get-bearing-to "klga" "kord"))))
    (with-fake-http [#"fpl" "{}"]
      (is (= 281 (get-bearing-to "klga" "kord"))))
    (clear-cache)))

(deftest get-exit-test
  (testing "Get exit"
    (is (= :west (get-exit-to sop-exits-klga 281)))
    (is (= :north (get-exit-to sop-exits-klga 290)))
    (is (= :north (get-exit-to sop-exits-klga 360)))
    (is (= :north (get-exit-to sop-exits-klga 000)))
    (is (= :east (get-exit-to sop-exits-klga 50)))
    (is (= :east (get-exit-to sop-exits-klga 90)))
    (is (= :south (get-exit-to sop-exits-klga 180)))))
