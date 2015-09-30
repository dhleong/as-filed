
(ns asfiled.metar-test
  (:require [clojure.test :refer :all]
            [clj-time.core :as t]
            [asfiled.metar :refer :all]))

(def metar-simple
  "KLGA 171951Z 17017G22KT 10SM FEW050 SCT160 BKN230 26/14 A3005 RMK AO2 SLP176 T02610144")

(def metar-rvrs
  "KLGA 171951Z 17017G22KT 4SM R22L/3000M2500FT +RA BKN230 26/14 A2991 RMK AO2 SLP176 T02610144")

(defn eq-by-key
  [a b]
  (every? true? 
          (map #(or 
                  (nil? (get a %))
                  (= (get a %) (get b %)))
               (keys a))))

(deftest time-test
  (testing "042023Z"
    (let [date (decode-time "042023Z")]
      (is (= 4 (t/day date)))
      (is (= 20 (t/hour date)))
      (is (= 23 (t/minute date))))))

(deftest wind-test
  (testing "eq-by-key"
    (is (not (eq-by-key {:speed 1 :dir 234} {:speed 2 :dir 234})))
    (is (eq-by-key {:speed 1} {:speed 1 :dir nil})))
  (testing "Simple"
    (is (eq-by-key {:speed 1 :dir 234} (decode-wind "23401KT"))))
  (testing "Gusts"
    (is (eq-by-key {:speed 1 :gust 10 :dir 234}
                   (decode-wind "23401G10KT"))))
  (testing "VRB winds"
    (is (eq-by-key {:speed 1 :dir :variable}
                   (decode-wind "VRB01KT"))))
  (testing "Variable winds"
    (is (eq-by-key {:speed 1 :dir 0 :dir-variable '(0 30)}
                   (decode-wind "000V03001KT"))))
  (testing "Everything"
    (is (eq-by-key {:speed 1 :dir 0 
                    :gust 20 :dir-variable '(0 30)}
                   (decode-wind "000V03001G20KT")))))

(deftest visibility-test
  (testing "Visbility"
    (is (= 2 (decode-visibility "2SM")))
    (is (= 30 (decode-visibility "30SM")))
    (is (= 1/4 (decode-visibility "1/4SM")))))

(deftest weather-test
  (testing "Simple"
    (is (= "Mist" (decode-weather "BR")))
    (is (= "Hail" (decode-weather "GR"))))
  (testing "Intensity"
    (is (= "Heavy Spray" (decode-weather "+PY")))
    (is (= "Light Snow" (decode-weather "-SN"))))
  (testing "Modifiers"
    (is (= "Patches of Spray" (decode-weather "BCPY")))
    (is (= "Showers of Snow" (decode-weather "SHSN"))))
  (testing "All together now!"
    (is (= "Heavy Thunderstorms with Hail" (decode-weather "+TSGR")))
    (is (= "Light Freezing Rain" (decode-weather "-FZRA")))))

(deftest sky-test
  (testing "Clear"
    (is (eq-by-key {:type :clear} (decode-sky "SKC")))
    (is (eq-by-key {:type :clear} (decode-sky "CLR"))))
  (testing "Ceiling"
    (is (eq-by-key {:type :indefinite :ceiling 2000} (decode-sky "VV020")))
    (is (eq-by-key {:type :few :ceiling 700} (decode-sky "FEW007"))))
  (testing "With Clouds"
    (is (= {:type :overcast :ceiling 2000 :clouds :cumulonimbus}
           (decode-sky "OVC020CB")))
    (is (= {:type :scattered :ceiling 700 :clouds :towering-cumulus}
           (decode-sky "SCT007TCU")))))

(deftest temperature-test
  (testing "Positive"
    (is (= {:value 20 :dewpoint 18} 
           (decode-temperature "20/18"))))
  (testing "Negative"
    (is (= {:value -20 :dewpoint -18} 
           (decode-temperature "M20/M18")))))

(deftest altimeter-test
  (testing "Altimeter"
    (is (= 2992 (decode-altimeter "A2992")))))

(deftest rvr-test
  (testing "Variable"
    (is (= {:runway "22" :visibility {:from 2000
                                      :to 3500
                                      :as :variable}}
           (decode-rvr "R22/2000V3500FT")))
    (is (= {:runway "04R" :visibility {:from 2000
                                       :to 3500
                                       :as :more-than}}
           (decode-rvr "R04R/2000P3500FT")))
    (is (= {:runway "22L" :visibility {:from 3000
                                       :to 2500
                                       :as :less-than}}
           (decode-rvr "R22L/3000M2500FT"))))
  (testing "Less-than"
    (is (= {:runway "22" :visibility {:is 2000
                                      :as :less-than}}
           (decode-rvr "R22/M2000FT"))))
  (testing "Variable + Greater Than"
    ;; this was observed
    (is (= {:runway "22" :visibility {:from 2000
                                      :to 3500
                                      :as :more-than}}
           (decode-rvr "R22/2000VP3500FT")))))

(deftest metar-test
  (testing metar-simple
    (let [metar (decode-metar metar-simple)]
      (is (not (nil? metar)))
      (is (= "KLGA" (:icao metar)))
      (is (= 51 (-> metar :time (t/minute))))
      (is (eq-by-key {:speed 17 :gust 22 :dir 170} 
                     (-> metar :wind)))
      (is (= 10 (:visibility metar)))
      (is (nil? (:weather metar)))
      (is (= 3 (-> metar :sky count)))
      (is (eq-by-key {:type :few :ceiling 5000} (-> metar :sky first)))
      (is (= 26 (-> metar :temperature :value)))
      (is (= 14 (-> metar :temperature :dewpoint)))
      (is (= 3005 (-> metar :altimeter)))
      (is (= 180 (-> metar :min-flight-level)))))
  (testing metar-rvrs
    (let [metar (decode-metar metar-rvrs)]
      (is (not (nil? metar)))
      (is (eq-by-key {:runway "22L"
                      :visibility {:from 3000
                                   :to 2500
                                   :as :less-than}}
                     (:rvr metar)))
      (is (vector? (-> metar :weather)))
      (is (= "Heavy Rain" (-> metar :weather first)))
      (is (vector? (-> metar :sky)))
      (is (= 1 (-> metar :sky count)))
      (is (= 190 (-> metar :min-flight-level))))))
