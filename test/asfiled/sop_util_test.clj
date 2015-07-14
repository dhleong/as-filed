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
    (is (not (match-tag :bar :foo))))
  (testing "any"
    (is (true? (match-tag :foo {:any [:bar :foo]})))
    (is (not (match-tag :baz {:any [:bar :foo]}))))
  (testing "not"
    (is (true? (match-tag :baz {:not [:bar :foo]})))
    (is (not (match-tag :foo {:not [:bar :foo]})))))

(deftest select-sid-test
  (testing "lga 13/22; jfk 13/ils22"
    (let [desc (select-sid
                 sop-klga 
                 [:lga-depart-13 :lga-land-22
                  :jfk-depart-13 :jfk-land-ils22])]
      (is (true? (-> desc (.contains "All Types"))))
      (is (true? (-> desc (.contains "All Gates"))))
      (is (true? (-> desc (.contains "FLUSHING"))))
      (is (true? (-> desc (.contains "TNNIS"))))
      )))
