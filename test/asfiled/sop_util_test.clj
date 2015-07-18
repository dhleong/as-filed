(ns asfiled.sop-util-test
  (:require [clojure.test :refer :all]
            [asfiled
             [sop-util :refer :all]
             [nyartcc-sop :refer [sop-klga sop-runways-kjfk]]]))

(def weather-calm {:speed 2 :dir 140})

(deftest match-runway-test
  (testing "JFK matching"
    (let [weather {:speed 14 :dir 190}]
      (is (not (match-runway 
                    weather 
                    (-> sop-runways-kjfk (nth 0))))) ;; 0-4 @ ANY
      (is (not (match-runway 
                    weather 
                    (-> sop-runways-kjfk (nth 1)))))))) ;; 5+ @ 0-99

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
    (is (not (match-tag :foo {:not [:bar :foo]}))))
  (testing "from sop"
    (is (true? (match-tag 
                 :jfk-land-ils22 
                 (-> sop-klga :sid-selection second :when last))))))

(deftest match-sid-test
  (testing "lga 13/22; jfk 13/ils22"
    (let [match (match-sid
                 [:lga-depart-13 :lga-land-22
                  :jfk-depart-13 :jfk-land-ils22]
                 (-> sop-klga :sid-selection second))]
      (is (true? match)))))

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
      (is (false? (-> desc (.contains "WHITESTONE"))))
      (is (false? (-> desc (.contains "\n"))))))
  (testing "lga 13/22; jfk 22r/4"
    (let [desc (select-sid
                 sop-klga 
                 [:lga-depart-13 :lga-land-22
                  :jfk-depart-22r :jfk-land-4])]
      (is (true? (-> desc (.contains "CONEY"))))
      (is (true? (-> desc (.contains "MASPETH"))))
      (is (true? (-> desc (.contains "WHITESTONE"))))
      (is (true? (-> desc (.contains "NTHNS"))))
      (is (true? (-> desc (.contains "GLDMN"))))
      (is (true? (-> desc (.contains "TNNIS"))))
      (is (false? (-> desc (.contains "\n\n"))))))
  (testing "lga 22/22; jfk 31/31"
    (let [desc (select-sid
                 sop-klga 
                 [:lga-depart-22 :lga-land-22
                  :jfk-depart-31 :jfk-land-31])]
      (is (true? (-> desc (.contains "All Types"))))
      (is (true? (-> desc (.contains "All Gates"))))
      (is (true? (-> desc (.contains "As Published"))))
      (is (true? (-> desc (.contains "JUTES"))))
      (is (false? (-> desc (.contains "\n")))))))
