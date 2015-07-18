(ns ^{:author "Daniel Leong"
      :doc "METAR decoder"}
  asfiled.metar
  (:require [clojure.string :refer [split]]
            [clj-time
             [core :as t]
             [format :as f]]))

;;
;; Constants
;;

(def weather-types
  {;; obscuration
   "BR" "Mist"
   "FG" "Fog"
   "DU" "Dust"
   "HZ" "Haze"
   "SA" "Sand"
   "PY" "Spray"
   "FU" "Smoke"
   ;; "qualifiers"; we add some extra words for convenience
   "MI" "Shallow"
   "BC" "Patches of"
   "BL" "Blowing"
   "DR" "Low Drifting"
   "SH" "Showers of"
   "TS" "Thunderstorms with"
   "FZ" "Freezing"
   "PR" "Partial"
   ;; precipitation
   "DZ" "Drizzle"
   "RA" "Rain"
   "SN" "Snow"
   "SG" "Snow Grains"
   "IC" "Ice Crystals"
   "PL" "Ice Pellets"
   "GR" "Hail"
   "GS" "Small Hail/Snow Pellets"
   "UP" "Unknown Precipitation"
   ;; others
   "PO" "Well-Developed Dust/Sand Whirls"
   "SQ" "Squalls"
   "FC" "Funnel Cloud/Tornado/Waterspout"
   "SS" "Sandstorm/Duststorm"})

(def sky-conditions
  {"SCT" :scattered
   "BKN" :broken
   "OVC" :overcast
   "FEW" :few})

;;
;; Utils
;;

(defn- as-int
  "Best-effort conversion to an Int"
  [raw]
  (try
    (Integer/parseInt raw)
    (catch NumberFormatException e
      raw)))

(defn- as-number
  "Best-effort conversion to a (possibly fractional) number"
  [raw]
  (if (.contains raw "/")
    (let [[numer denom] (split raw #"/")]
      (/ (as-int numer) (as-int denom)))
    ;; safe fallback
    (as-int raw)))

(defn- as-feet
  "Best-effort conversion from flight level to feet"
  [raw]
  (-> raw as-int (* 100)))

;;
;; Decoder parts
;;

(defn decode-time
  [token]
  (let [today (t/today-at-midnight)
        base (f/parse (f/formatter "ddHHmmZ") token)]
    (t/date-time 
      (t/year today)
      (t/month today)
      (t/day base)
      (t/hour base)
      (t/minute base))))

(defn decode-wind
  [token]
  (let [speed (re-find #"(\d\d)(G(\d\d))?KT$" token)
        dir (re-find #"VRB|(\d\d\d)V?(\d\d\d)?" token)]
    {:speed (as-int (second speed))
     :gust (as-int (nth speed 3 nil))
     :dir (if (= "VRB" (first dir)) 
            :variable
            (as-int (second dir)))
     :dir-variable (if (last dir)
                     (map as-int (rest dir)))}))

(defn decode-visibility
  [token]
  (-> token
      (.substring 0 (- (count token) 2))
      as-number))

(defn decode-weather
  [token]
  (let [raw (->> token (take-last 2) (apply str))
        modif (->> token (take-last 4) (take 2) (apply str))
        desc (get weather-types raw)
        desc-with-mod (let [modif-desc (get weather-types modif)]
                        (if (and modif-desc (not= modif raw))
                          (str modif-desc " " desc)
                          desc))]
    (case (first token)
      \+ (str "Heavy " desc-with-mod)
      \- (str "Light " desc-with-mod)
      desc-with-mod)))

(defn decode-sky
  [token]
  (let [base (-> token (.replaceAll "(CB|TCU|ACC)$", ""))
        clouds
        (cond 
          (-> token (.endsWith "CB")) :cumulonimbus
          (-> token (.endsWith "TCU")) :towering-cumulus
          (-> token (.endsWith "ACC")) :altocumulus-castellanus
          :else nil)]
    (merge 
      {:clouds clouds} 
      (cond
        (= "SKC" base) {:type :clear}
        (= "CLR" base) {:type :clear}
        (-> token (.startsWith "VV")) {:type :indefinite 
                                       :ceiling (-> base (.substring 2) as-feet)}
        :else {:type (get sky-conditions (-> base (.substring 0 3)))
               :ceiling (-> base (.substring 3) as-feet)}))))

(def metar-parts
  {:time [#"[0-9]+Z" decode-time]
   :wind [#"[0-9GVRB]+KT" decode-wind]
   :visibility [#"[0-9/]+SM" decode-visibility]
   :weather [#"(+|-)?[A-Z]{2,4}" decode-weather]
   :sky [#"(SKC|CLR|[A-Z]{2,3}[0-9]{3})([TCUBA]{2,3})?" decode-sky]})


;;
;; Public API
;;

(defn decode-metar
  "Decode a metar"
  [metar]
  (let [parts (split metar #" +")
        icao (first parts)]
    parts))
