(ns asfiled.api-test
  (:require [clojure.test :refer :all]
            [asfiled.api :refer :all]
            [asfiled.sink :refer [Sink]]))

(def dummy-sink 
  (reify Sink
    (get-facility [this] "KLGA")
    (get-aircraft [this aircraft]
      {:model "A-321"
       :manufacturer "Airbus"
       :type "Airplane"
       :engines "2 Jet engines"
       :weight "Large"})
    (get-airline [this airline]
      {:name "British Airways"
       :telephony "Speedbird"})
    (get-airport [this icao]
      {:name "Washington Dulles International Aiport"
       :icao "KIAD" })
    (get-preferred-routes [this from to]
      [{:route "PARKE HYPER#"
        :area ""
        :aircraft "RNAV"
        :alititude "FL180-FL220"
        :preferred true }])
    (get-valid-exits [this to]
      {:bearing 238
       :gate :west
       :exits ["LANNA"]})))

(def client
  {:callsign "BAW123"
   :craft "T/A321/L"
   :depart "KLGA"
   :arrive "KIAD"})

(deftest utils-test
  (testing "airline-name"
    (is (= "BAW" (airline-name client))))
  (testing "aircraft-type"
    (is (= "A321" (aircraft-type client)))
    (is (= "A321" (aircraft-type {:craft "A321"})))
    (is (= "A321" (aircraft-type {:craft "A321/L"}))))
  (testing "equipment-type"
    (let [data (equipment-type client)]
      (is (identity (:rvsm data)))
      (is (= :mode-c (:transponder data)))
      (is (= "GNSS" (:nav data))))
    (let [data (equipment-type {:craft "A321/L"})]
      (is (identity (:rvsm data)))
      (is (= :mode-c (:transponder data)))
      (is (= "GNSS" (:nav data))))
    (let [data (equipment-type {:craft "A321/G"})]
      (is (not (identity (:rvsm data))))
      (is (= :mode-c (:transponder data)))
      (is (= "GNSS" (:nav data))))
    ;; this is basically a safety check...
    (is (nil? (equipment-type {:craft "A321"})))))

(deftest analyze-test
  (testing "analyze"
    (let [result (analyze dummy-sink client)]
      (is (= "Speedbird" (-> result :airline :telephony)))
      (is (= "A-321" (-> result :craft :model)))
      (is (= 1 (-> result :preferred-routes count)))
      (is (= :mode-c (-> result :equip :transponder)))
      (is (= :west (-> result :exits :gate)))))) 
