(ns asfiled.vatsim-test
  (:use org.httpkit.fake)
  (:require [clojure.test :refer :all]
            [asfiled.vatsim :refer :all]))

(def data-urls-raw
  "url0=http://vatsim.aircharts.org/vatsim-data.txt\nurl0=http://vatsim-data.hardern.net/vatsim-data.txt")

(deftest data-urls-test
  (testing "Load data urls"
    (with-fake-http [#"status.txt$" data-urls-raw]
      (let [loaded (get-data-urls)]
        (is (= 2 (count loaded)))
        (is (= "http://vatsim.aircharts.org/vatsim-data.txt" (first loaded)))
        (is (= "http://vatsim-data.hardern.net/vatsim-data.txt" (second loaded))))))
  (testing "Load from cache"
    (with-fake-http []
      (let [loaded (get-data-urls)]
        (is (= 2 (count loaded)))
        (is (= "http://vatsim.aircharts.org/vatsim-data.txt" (first loaded)))
        (is (= "http://vatsim-data.hardern.net/vatsim-data.txt" (second loaded)))))))
