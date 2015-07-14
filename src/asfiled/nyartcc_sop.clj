(ns ^{:author "Daniel Leong"
      :doc "SOPs for nyartcc"}
  asfiled.nyartcc-sop
  (:require [clojure.string :refer [upper-case join]]))

(def sop-runways-kjfk
  [{:when {:speed [0 4] :dir [[0 360]]}
    :use "JFK: Depart: 31L/R  Land: 31L/R"
    :tags [:jfk-depart-31 :jfk-land-31]}])

(def lga-land-catchall
  {:not [:lga-land-22]})
(def jfk-depart-catchall
  {:any [:jfk-depart-22r :jfk-depart-13 :jfk-depart-4l]})
(def jfk-land-catchall
  {:any [:jfk-land-4 :jfk-land-dme22l :jfk-land-22l]})

(def sop-klga
  {:exit-intervals
   {:north [[290 360] [0 15]]
    :east [[16 95]]
    :south [[96 215]]
    :west [[216 289]]}
   :exit-gates
   {:north ["GAYEL" "HAAYS" "NEION" "COATE" "SAX" "NYACK"]
    :east ["GREKI" "MERIT" "BAYYS" "CMK" "BDR"]
    :south ["SHIPP" "WAVEY" "DIXIE" "WHITE" "DPK"]
    :west ["NEWEL" "ELIOT" "ZIMMZ!" "PARKE" "LANNA" "BIGGY" "SBJ"]}
   :runway-selection
   (concat
     [{:when {:speed [0 4] :dir [[0 360]]}
       :use "LGA: Depart: 31  Land: VMC 22, IMC ILS22"
       :tags [:lga-depart-31 :lga-land-22]}
      {:when {:speed [5 14] :dir [[315 360] [0 44]]}
       :use "LGA: Depart: 4  Land: VMC EXP31, IMC LOC31"
       :tags (:lga-depart-4 :lga-land-31)}
      ]
     ;; merge with JFK to get its tags
     sop-runways-kjfk)
   :sid-selection
   [{:when [:lga-depart-13 lga-land-catchall
            :jfk-depart-13 :jfk-land-ils13]
     :use "(All Types) (All Gates) [FLUSHING] [TNNIS#]"}
    {:when [:lga-depart-13 lga-land-catchall
            :jfk-depart-13 :jfk-land-ils22]
     :use "(All Types) (All Gates) [FLUSHING] [TNNIS#]"}
    {:when [:lga-depart-13 lga-land-catchall
            :jfk-depart-13 :jfk-land-vor13]
     :use "(All Types) (All Gates) [WHITESTONE] [TNNIS#]"}
    {:when [:lga-depart-13 lga-land-catchall
            :jfk-depart-31 :jfk-land-ils22]
     :use "(All Types) (All Gates) [FLUSHING] [TNNIS#]"}
    {:when [:lga-depart-13 lga-land-catchall
            :jfk-depart-31 {:not [:jfk-land-ils22]}]
     :use "(All Types) (All Gates) [WHITESTONE] [TNNIS#]"}
    {:when [:lga-depart-13 lga-land-catchall
            :jfk-depart-22r :jfk-land-ils22]
     :use (join "\n"
                ["JETS (South Gates) [WHITESTONE] [NTHNS#]"
                 "JETS (W/N/E Gates) [MASPETH]    [GLDMN#]"
                 "PROP (All Gates)   [FLUSHING**] [TNNIS#]"])}
    {:when [:lga-depart-13 :lga-land-22
            jfk-depart-catchall jfk-land-catchall]
     :use (join "\n"
                ["JETS (South Gates) [CONEY]      [NTHNS#]"
                 "JETS (W/N   Gates) [MASPETH]    [GLDMN#]"
                 "JETS (East  Gates) [WHITESTONE] [TNNIS#]"
                 "PROP (All Gates)   [WHITESTONE] [TNNIS#]"])}
    {:when [:lga-depart-13 lga-land-catchall
            jfk-depart-catchall jfk-land-catchall]
     :use (join "\n"
                ["JETS (South Gates) [CONEY]      [NTHNS#]"
                 "JETS (W/N/E Gates) [WHITESTONE] [TNNIS#]"
                 "PROP (All Gates)   [WHITESTONE] [TNNIS#]"])}
    {:when [:lga-depart-22 :lga-land-22
            :jfk-depart-31 :jfk-land-31]
     :use "(All Types) (All Gates) [As Publish] [JUTES#]"}
    {:when [:lga-depart-22 :lga-land-22
            {:not [:jfk-depart-31]} :jfk-land-13]
     :use "(All Types) (All Gates) [As Publish] [JUTES#]"}
    {:when [:lga-depart-22 :lga-land-22
            {:not [:jfk-depart-31]} {:any [:jfk-land-4 :jfk-land-22]}]
     :use (join "\n"
                ["ALL  (W/N/E Gates) [As Publish] [JUTES#]"
                 "JETS (South Gates) [Mimic RNAV] [HOPEA#]"])}
    ]
   })

;; NB: For runway selection, we examine ALL options
;;  and evaluate ALL matches. This means we merge
;;  the tags and merge the :use strings, in the
;;  order they were accepted. The :use strings
;;  are joined with a newline
;; For SID selection, each item in the :when array
;;  is tested against the set of tags. Items in this
;;  array may be:
;;   - A single symbol/string: Exact match (convenience)
;;   - {:not [symbols]}: Anything BUT the symbols
;;   - {:any [symbols]}: ANY of the symbols
;;  Then, ALL matching configurations are returned.

(def sop-map
  {"KLGA" sop-klga})

(defn get-sop [facility-icao]
  (get sop-map (upper-case facility-icao)))
