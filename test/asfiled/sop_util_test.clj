(ns asfiled.sop-util-test
  (:require [clojure.test :refer :all]
            [asfiled
             [sop-util :refer :all]
             [nyartcc-sop :refer [sop-klga]]]))

(def weather-calm {:speed 2 :dir 140})

(deftest select-runways-test
  (testing "calm winds"
    (let [config (select-runways sop-klga weather-calm)
          text (:runways config)]
      (is (= [:lga-depart-31 :lga-land-22 :jfk-depart-31 :jfk-land-31]
             (:tags config)))
      (is (= 0 (-> text (.indexOf "LGA: Depart: 31")))))))

(deftest match-tag-test
  (testing "exact"
    (is (true? (match-tag :foo :foo)))
    (is (false? (match-tag :bar :foo))))
  (testing "any"
    (is (true? (match-tag :foo {:any [:bar :foo]})))
    (is (false? (match-tag :bar {:any [:bar :foo]})))
    ))
