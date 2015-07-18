
(ns asfiled.metar-test
  (:require [clojure.test :refer :all]
            [clj-time.core :as t]
            [asfiled.metar :refer :all]))

(def metar-simple
  "KLGA 171951Z 17017G22KT 10SM FEW050 SCT160 BKN230 26/14 A3005 RMK AO2 SLP176 T02610144")

(def metar-rvrs
  "KLGA 171951Z 17017G22KT 10SM FEW050 SCT160 BKN230 26/14 A3005 RMK AO2 SLP176 T02610144")

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

(deftest metar-test
  (testing metar-simple
    (let [metar (decode-metar metar-simple)]
      (is (not (nil? metar))))))
